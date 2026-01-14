#include "Block.h"
#include "Blockchain.h"
#include "Miner.h"
#include <iostream>
#include <string>
#include <ctime>
#include <sstream>
#include <mpi.h>

// MPI Tags for different message types
const int TAG_BLOCK_DATA = 1;
const int TAG_CHAIN_SYNC = 2;
const int TAG_MINING_SIGNAL = 3;

void printMenu() {
    std::cout << "\n========== BLOCKCHAIN MPI NODE ==========" << std::endl;
    std::cout << "1. Mine new block" << std::endl;
    std::cout << "2. Show blockchain" << std::endl;
    std::cout << "3. Validate blockchain" << std::endl;
    std::cout << "4. Show chain info" << std::endl;
    std::cout << "5. Sync chain from other nodes" << std::endl;
    std::cout << "6. Exit" << std::endl;
    std::cout << "=========================================" << std::endl;
    std::cout << "Enter choice: ";
}

void printBlock(const Block& block) {
    std::cout << "\n--- Block #" << block.index << " ---" << std::endl;
    std::cout << "Data: " << block.data << std::endl;
    std::cout << "Timestamp: " << block.timestamp << std::endl;
    std::cout << "Previous Hash: " << block.previousHash.substr(0, 16) << "..." << std::endl;
    std::cout << "Hash: " << block.hash.substr(0, 16) << "..." << std::endl;
    std::cout << "Difficulty: " << block.difficulty << std::endl;
    std::cout << "Nonce: " << block.nonce << std::endl;
}

void printChain(const Blockchain& blockchain) {
    std::cout << "\n========== BLOCKCHAIN ==========" << std::endl;
    for (const auto& block : blockchain.getChain()) {
        printBlock(block);
    }
    std::cout << "================================" << std::endl;
}

void printChainInfo(const Blockchain& blockchain) {
    std::cout << "\n========== CHAIN INFO ==========" << std::endl;
    std::cout << "Length: " << blockchain.getChain().size() << std::endl;
    std::cout << "Current Difficulty: " << blockchain.getDifficulty() << std::endl;
    std::cout << "Cumulative Difficulty: " << blockchain.getCumulativeDifficulty(blockchain.getChain()) << std::endl;
    
    const Block& latest = blockchain.getLatestBlock();
    std::cout << "Latest Block:" << std::endl;
    std::cout << "  Index: " << latest.index << std::endl;
    std::cout << "  Hash: " << latest.hash << std::endl;
    std::cout << "  Timestamp: " << latest.timestamp << std::endl;
    std::cout << "================================" << std::endl;
}

// Broadcast a newly mined block to all other MPI nodes
void broadcastBlock(const Block& block, int mpiRank, int mpiSize) {
    std::string blockJson = block.toJson();
    int jsonSize = blockJson.length();
    
    // Broadcast the size first
    MPI_Bcast(&jsonSize, 1, MPI_INT, mpiRank, MPI_COMM_WORLD);
    
    // Broadcast the JSON data
    char* buffer = const_cast<char*>(blockJson.c_str());
    MPI_Bcast(buffer, jsonSize + 1, MPI_CHAR, mpiRank, MPI_COMM_WORLD);
    
    std::cout << "[MPI] Block #" << block.index << " broadcast to all nodes" << std::endl;
}

// Receive a block broadcast from another node
Block receiveBlockBroadcast(int sourceRank) {
    int jsonSize;
    MPI_Bcast(&jsonSize, 1, MPI_INT, sourceRank, MPI_COMM_WORLD);
    
    char* buffer = new char[jsonSize + 1];
    MPI_Bcast(buffer, jsonSize + 1, MPI_CHAR, sourceRank, MPI_COMM_WORLD);
    buffer[jsonSize] = '\0';
    
    std::string blockJson(buffer);
    delete[] buffer;
    
    return Block::fromJson(blockJson);
}

// Synchronize blockchain across all nodes
void synchronizeChain(Blockchain& blockchain, int mpiRank, int mpiSize) {
    if (mpiSize == 1) return; // Single node, nothing to sync
    
    std::cout << "[MPI] Synchronizing blockchain across all nodes..." << std::endl;
    
    // Each node shares its chain length and cumulative difficulty
    int localLength = blockchain.getChain().size();
    double localCumulativeDiff = blockchain.getCumulativeDifficulty(blockchain.getChain());
    
    std::vector<int> allLengths(mpiSize);
    std::vector<double> allDifficulties(mpiSize);
    
    MPI_Allgather(&localLength, 1, MPI_INT, allLengths.data(), 1, MPI_INT, MPI_COMM_WORLD);
    MPI_Allgather(&localCumulativeDiff, 1, MPI_DOUBLE, allDifficulties.data(), 1, MPI_DOUBLE, MPI_COMM_WORLD);
    
    // Find the node with the best chain (highest cumulative difficulty)
    int bestNode = 0;
    double bestDifficulty = allDifficulties[0];
    for (int i = 1; i < mpiSize; i++) {
        if (allDifficulties[i] > bestDifficulty) {
            bestDifficulty = allDifficulties[i];
            bestNode = i;
        }
    }
    
    if (mpiRank == 0) {
        std::cout << "[MPI] Best chain is on Node " << bestNode 
                  << " (cumulative difficulty: " << bestDifficulty << ")" << std::endl;
    }
    
    // Broadcast the best chain from the best node
    if (mpiRank == bestNode) {
        std::string chainJson = blockchain.toJson();
        int jsonSize = chainJson.length();
        
        MPI_Bcast(&jsonSize, 1, MPI_INT, bestNode, MPI_COMM_WORLD);
        
        char* buffer = const_cast<char*>(chainJson.c_str());
        MPI_Bcast(buffer, jsonSize + 1, MPI_CHAR, bestNode, MPI_COMM_WORLD);
        
        std::cout << "[MPI] Broadcasting chain to all nodes" << std::endl;
    } else {
        int jsonSize;
        MPI_Bcast(&jsonSize, 1, MPI_INT, bestNode, MPI_COMM_WORLD);
        
        char* buffer = new char[jsonSize + 1];
        MPI_Bcast(buffer, jsonSize + 1, MPI_CHAR, bestNode, MPI_COMM_WORLD);
        buffer[jsonSize] = '\0';
        
        std::string chainJson(buffer);
        delete[] buffer;
        
        std::vector<Block> receivedChain = Blockchain::chainFromJson(chainJson);
        
        if (blockchain.replaceChain(receivedChain)) {
            std::cout << "[MPI] Chain synchronized from Node " << bestNode << std::endl;
        } else {
            std::cout << "[MPI] Kept local chain (already synchronized)" << std::endl;
        }
    }
}

int main(int argc, char* argv[]) {
    // Initialize MPI
    MPI_Init(&argc, &argv);
    
    int mpiRank, mpiSize;
    MPI_Comm_rank(MPI_COMM_WORLD, &mpiRank);
    MPI_Comm_size(MPI_COMM_WORLD, &mpiSize);
    
    // Only rank 0 prints the header
    if (mpiRank == 0) {
        std::cout << "========================================" << std::endl;
        std::cout << "   BLOCKCHAIN MPI - C++ Implementation" << std::endl;
        std::cout << "========================================" << std::endl;
        std::cout << "MPI Cluster Size: " << mpiSize << " node(s)" << std::endl;
        std::cout << "Threads per node: " << Miner::getOptimalThreadCount(mpiSize) << std::endl;
        std::cout << "Total workers: " << (mpiSize * Miner::getOptimalThreadCount(mpiSize)) << std::endl;
        std::cout << "========================================" << std::endl;
    }
    
    // Each node has its own blockchain
    Blockchain blockchain;
    
    if (mpiRank == 0) {
        std::cout << "\n[INIT] Genesis block created on all nodes." << std::endl;
    }
    
    // Synchronize at start
    MPI_Barrier(MPI_COMM_WORLD);
    
    // Main loop - only rank 0 handles user input
    bool running = true;
    while (running) {
        int choice = 0;
        
        if (mpiRank == 0) {
            printMenu();
            std::cin >> choice;
            
            if (std::cin.fail()) {
                std::cin.clear();
                std::cin.ignore(10000, '\n');
                std::cout << "Invalid input. Please enter a number." << std::endl;
                choice = 0;
            }
        }
        
        // Broadcast the choice to all nodes
        MPI_Bcast(&choice, 1, MPI_INT, 0, MPI_COMM_WORLD);
        
        switch (choice) {
            case 1: {
                // Mine new block - ALL nodes participate
                std::string data;
                
                if (mpiRank == 0) {
                    std::cout << "Enter block data: ";
                    std::cin.ignore();
                    std::getline(std::cin, data);
                }
                
                // Broadcast the data to all nodes
                int dataSize = data.length();
                MPI_Bcast(&dataSize, 1, MPI_INT, 0, MPI_COMM_WORLD);
                
                if (mpiRank != 0) {
                    data.resize(dataSize);
                }
                char* dataBuffer = const_cast<char*>(data.c_str());
                MPI_Bcast(dataBuffer, dataSize + 1, MPI_CHAR, 0, MPI_COMM_WORLD);
                
                const Block& previousBlock = blockchain.getLatestBlock();
                int difficulty = blockchain.getDifficulty(mpiRank); // Pass rank for logging control
                
                if (mpiRank == 0) {
                    std::cout << "\n[INFO] Mining block #" << (previousBlock.index + 1) 
                             << " with difficulty " << difficulty << std::endl;
                }
                
                // All nodes mine in parallel
                Block minedBlock = Miner::mineBlock(
                    previousBlock.index + 1,
                    data,
                    previousBlock.hash,
                    difficulty,
                    mpiRank,
                    mpiSize
                );
                
                // Determine which node found it
                int foundRank = -1;
                if (!minedBlock.hash.empty() && Miner::hashMeetsDifficulty(minedBlock.hash, difficulty)) {
                    foundRank = mpiRank;
                }
                
                int globalFoundRank;
                MPI_Allreduce(&foundRank, &globalFoundRank, 1, MPI_INT, MPI_MAX, MPI_COMM_WORLD);
                
                // The node that found it broadcasts the block
                if (globalFoundRank >= 0) {
                    if (mpiRank == globalFoundRank) {
                        broadcastBlock(minedBlock, mpiRank, mpiSize);
                    } else {
                        minedBlock = receiveBlockBroadcast(globalFoundRank);
                    }
                    
                    // All nodes add the block
                    if (blockchain.addBlock(minedBlock)) {
                        if (mpiRank == 0) {
                            std::cout << "[SUCCESS] Block added to blockchain on all nodes!" << std::endl;
                        }
                    } else {
                        if (mpiRank == 0) {
                            std::cout << "[ERROR] Failed to add block!" << std::endl;
                        }
                    }
                } else {
                    if (mpiRank == 0) {
                        std::cout << "[ERROR] Mining failed on all nodes!" << std::endl;
                    }
                }
                
                break;
            }
            
            case 2: {
                // Show blockchain - only rank 0
                if (mpiRank == 0) {
                    printChain(blockchain);
                }
                break;
            }
            
            case 3: {
                // Validate blockchain - all nodes
                bool isValid = blockchain.isValidChain(blockchain.getChain());
                
                if (mpiRank == 0) {
                    std::cout << "\n[VALIDATION] Validating blockchain..." << std::endl;
                    if (isValid) {
                        std::cout << "[SUCCESS] Blockchain is valid on Node 0!" << std::endl;
                    } else {
                        std::cout << "[ERROR] Blockchain is invalid on Node 0!" << std::endl;
                    }
                }
                
                // Check all nodes
                int localValid = isValid ? 1 : 0;
                int totalValid;
                MPI_Reduce(&localValid, &totalValid, 1, MPI_INT, MPI_SUM, 0, MPI_COMM_WORLD);
                
                if (mpiRank == 0) {
                    std::cout << "[INFO] Valid on " << totalValid << "/" << mpiSize << " nodes" << std::endl;
                }
                
                break;
            }
            
            case 4: {
                // Show chain info - only rank 0
                if (mpiRank == 0) {
                    printChainInfo(blockchain);
                }
                break;
            }
            
            case 5: {
                // Sync chain from other nodes
                synchronizeChain(blockchain, mpiRank, mpiSize);
                break;
            }
            
            case 6: {
                // Exit
                if (mpiRank == 0) {
                    std::cout << "\nExiting all nodes..." << std::endl;
                }
                running = false;
                break;
            }
            
            default: {
                if (mpiRank == 0 && choice != 0) {
                    std::cout << "Invalid choice. Please try again." << std::endl;
                }
                break;
            }
        }
        
        // Synchronize all nodes before next iteration
        MPI_Barrier(MPI_COMM_WORLD);
    }
    
    // Finalize MPI
    MPI_Finalize();
    return 0;
}
