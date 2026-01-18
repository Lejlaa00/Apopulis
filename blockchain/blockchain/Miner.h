#pragma once
#include "Block.h"
#include <string>
#include <atomic>
#include <thread>
#include <vector>

class Miner {
public:
    // Mine a new block with hybrid MPI+thread parallelization
    // Uses proper nonce partitioning to avoid overlap across all nodes/threads
    static Block mineBlock(int index, const std::string& data, 
                          const std::string& previousHash, int difficulty,
                          int mpiRank, int mpiSize);
    
    // Check if hash meets difficulty requirement
    static bool hashMeetsDifficulty(const std::string& hash, int difficulty);
    
    // Get optimal thread count for this architecture (considers MPI processes)
    static int getOptimalThreadCount(int mpiSize = 1);
    static void setThreadOverride(int threads);

private:
    // Thread worker function for parallel mining with MPI awareness
    static void mineWorker(Block* blockTemplate, int difficulty,
                          unsigned long long startNonce, unsigned long long stride,
                          std::atomic<bool>* found, std::atomic<unsigned long long>* resultNonce,
                          std::string* resultHash, std::atomic<unsigned long long>* hashCount,
                          int mpiRank, int mpiSize);
     static std::atomic<int> threadOverride;
};

