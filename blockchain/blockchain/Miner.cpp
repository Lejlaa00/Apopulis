#include "Miner.h"
#include <iostream>
#include <ctime>
#include <chrono>
#include <algorithm>
#include <climits>
#include <mpi.h>

int Miner::getOptimalThreadCount(int mpiSize) {
    unsigned int hwThreads = std::thread::hardware_concurrency();
    if (hwThreads == 0) {
        return 4; // fallback if detection fails
    }
    
    // Divide threads among MPI processes to avoid oversubscription
    // Reserve 1-2 cores for OS and other processes
    int availableThreads = hwThreads - 1;
    if (availableThreads < 1) availableThreads = 1;
    
    int threadsPerNode = availableThreads / mpiSize;
    
    // Ensure at least 1 thread per node
    if (threadsPerNode < 1) {
        threadsPerNode = 1;
    }
    
    // Cap at reasonable maximum (8 threads per node)
    if (threadsPerNode > 8) {
        threadsPerNode = 8;
    }
    
    return threadsPerNode;
}

void Miner::mineWorker(Block* blockTemplate, int difficulty,
                       unsigned long long startNonce, unsigned long long stride,
                       std::atomic<bool>* found, std::atomic<unsigned long long>* resultNonce,
                       std::string* resultHash, std::atomic<unsigned long long>* hashCount,
                       int mpiRank, int mpiSize) {
    Block localBlock = *blockTemplate;
    localBlock.nonce = startNonce;
    
    unsigned long long iterationCount = 0;
    const unsigned long long MPI_CHECK_INTERVAL = 10000; // Check MPI every 10k iterations
    
    while (!found->load()) {
        localBlock.hash = localBlock.calculateHash();
        (*hashCount)++;
        iterationCount++;
        
        if (hashMeetsDifficulty(localBlock.hash, difficulty)) {
            // Try to be the first to mark as found
            bool expected = false;
            if (found->compare_exchange_strong(expected, true)) {
                resultNonce->store(localBlock.nonce);
                *resultHash = localBlock.hash;
            }
            return;
        }
        
        // Periodically check if another MPI process found the solution
        // Only the first thread (thread 0) does MPI checks to avoid contention
        if (iterationCount % MPI_CHECK_INTERVAL == 0) {
            // Check if master thread signaled to stop
            if (found->load()) {
                return;
            }
        }
        
        localBlock.nonce += stride;
        
        // Prevent overflow wrap-around
        if (localBlock.nonce < startNonce) {
            return; // This thread exhausted its range
        }
    }
}

Block Miner::mineBlock(int index, const std::string& data, 
                       const std::string& previousHash, int difficulty,
                       int mpiRank, int mpiSize) {
    
    // Determine thread count (optimal for this architecture, considering MPI processes)
    int numThreads = getOptimalThreadCount(mpiSize);
    
    Block newBlock(index, data, previousHash, difficulty);
    
    if (mpiRank == 0) {
        std::cout << "[MINER] Starting to mine block #" << index 
                  << " with difficulty " << difficulty << std::endl;
        std::cout << "[MINER] MPI Cluster: " << mpiSize << " node(s)" << std::endl;
        std::cout << "[MINER] Threads per node: " << numThreads << std::endl;
        std::cout << "[MINER] Total parallel workers: " << (mpiSize * numThreads) << std::endl;
    }
    
    auto startTime = std::chrono::high_resolution_clock::now();
    
    // Atomic variables for thread coordination
    std::atomic<bool> found(false);
    std::atomic<unsigned long long> resultNonce(0);
    std::atomic<unsigned long long> totalHashCount(0);
    std::string resultHash;
    
    // Calculate nonce partitioning for this node's threads
    // Formula: global_thread_id = (rank * threads_per_node) + local_thread_id
    //          stride = total_ranks * threads_per_node
    unsigned long long globalStride = static_cast<unsigned long long>(mpiSize) * 
                                     static_cast<unsigned long long>(numThreads);
    
    // Launch worker threads
    std::vector<std::thread> workers;
    for (int localThreadId = 0; localThreadId < numThreads; localThreadId++) {
        // Each thread gets unique starting nonce across entire cluster
        unsigned long long globalThreadId = 
            static_cast<unsigned long long>(mpiRank) * numThreads + localThreadId;
        
        workers.emplace_back(mineWorker, &newBlock, difficulty,
                           globalThreadId, globalStride, &found, &resultNonce,
                           &resultHash, &totalHashCount, mpiRank, mpiSize);
    }
    
    // Master thread: periodically check if another MPI node found the solution
    std::thread mpiCheckThread([&found, mpiRank, mpiSize]() {
        if (mpiSize == 1) return; // Single node, no need to check
        
        const int MPI_CHECK_MS = 100; // Check every 100ms
        MPI_Status status;
        int flag;
        
        while (!found.load()) {
            std::this_thread::sleep_for(std::chrono::milliseconds(MPI_CHECK_MS));
            
            // Non-blocking check for messages from other nodes
            MPI_Iprobe(MPI_ANY_SOURCE, 0, MPI_COMM_WORLD, &flag, &status);
            
            if (flag) {
                // Another node found the solution
                found.store(true);
                return;
            }
        }
    });
    
    // Wait for all threads to complete
    for (auto& worker : workers) {
        worker.join();
    }
    
    mpiCheckThread.join();
    
    auto endTime = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::microseconds>(endTime - startTime);
    double elapsed = duration.count() / 1000000.0;
    
    // Check if THIS node found the solution
    bool localFound = found.load();
    int globalFoundRank = -1;
    
    if (mpiSize > 1) {
        // Determine which node found the solution
        int localFoundInt = localFound ? mpiRank : -1;
        MPI_Allreduce(&localFoundInt, &globalFoundRank, 1, MPI_INT, MPI_MAX, MPI_COMM_WORLD);
    } else {
        globalFoundRank = localFound ? 0 : -1;
    }
    
    if (globalFoundRank == mpiRank && localFound) {
        // This node found the solution
        newBlock.nonce = resultNonce.load();
        newBlock.hash = resultHash;
        newBlock.miningTime = elapsed;  // Store the actual mining time
        
        unsigned long long hashes = totalHashCount.load();
        double hashRate = elapsed > 0 ? hashes / elapsed : 0;
        
        std::cout << "[MINER] *** BLOCK FOUND by Node " << mpiRank << " ***" << std::endl;
        std::cout << "  Block #" << index << std::endl;
        std::cout << "  Nonce: " << newBlock.nonce << std::endl;
        std::cout << "  Hash: " << newBlock.hash << std::endl;
        std::cout << "  Mining time: " << elapsed << " seconds" << std::endl;
        std::cout << "  Local hash rate: " << hashRate << " H/s" << std::endl;
        std::cout << "  Local hashes: " << hashes << std::endl;
        
        // Broadcast the found block to all other nodes
        if (mpiSize > 1) {
            // Send signal that we found it
            int signal = 1;
            for (int dest = 0; dest < mpiSize; dest++) {
                if (dest != mpiRank) {
                    MPI_Send(&signal, 1, MPI_INT, dest, 0, MPI_COMM_WORLD);
                }
            }
        }
        
        return newBlock;
    } else if (globalFoundRank >= 0) {
        // Another node found the solution - receive it
        if (mpiRank == 0) {
            std::cout << "[MINER] Block found by Node " << globalFoundRank << std::endl;
        }
        
        // Receive signal (already probed, now receive it)
        int signal;
        MPI_Recv(&signal, 1, MPI_INT, globalFoundRank, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
        
        // The block will be broadcast separately via MPI_Bcast in main
        return newBlock; // Return empty block, will be filled by broadcast
    } else {
        std::cerr << "[MINER] ERROR: Mining failed - no solution found!" << std::endl;
        return newBlock;
    }
}

bool Miner::hashMeetsDifficulty(const std::string& hash, int difficulty) {
    if (difficulty <= 0) return true;
    
    std::string prefix(difficulty, '0');
    return hash.substr(0, difficulty) == prefix;
}

