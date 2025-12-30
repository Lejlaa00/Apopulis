#include "Miner.h"
#include <iostream>
#include <ctime>

Block Miner::mineBlock(int index, const std::string& data, 
                       const std::string& previousHash, int difficulty) {
    Block newBlock(index, data, previousHash, difficulty);
    
    std::cout << "[MINER] Starting to mine block #" << index 
              << " with difficulty " << difficulty << std::endl;
    
    time_t startTime = std::time(nullptr);
    unsigned long long hashCount = 0;
    
    while (true) {
        newBlock.hash = newBlock.calculateHash();
        hashCount++;
        
        if (hashMeetsDifficulty(newBlock.hash, difficulty)) {
            time_t endTime = std::time(nullptr);
            double elapsed = difftime(endTime, startTime);
            double hashRate = elapsed > 0 ? hashCount / elapsed : 0;
            
            std::cout << "[MINER] Block #" << index << " mined!" << std::endl;
            std::cout << "  Nonce: " << newBlock.nonce << std::endl;
            std::cout << "  Hash: " << newBlock.hash << std::endl;
            std::cout << "  Time: " << elapsed << " seconds" << std::endl;
            std::cout << "  Hash rate: " << hashRate << " H/s" << std::endl;
            std::cout << "  Total hashes: " << hashCount << std::endl;
            
            return newBlock;
        }
        
        newBlock.nonce++;
        
        // Print progress every 100000 hashes
        if (hashCount % 100000 == 0) {
            printMiningProgress(newBlock.nonce, newBlock.hash);
        }
    }
}

bool Miner::hashMeetsDifficulty(const std::string& hash, int difficulty) {
    if (difficulty <= 0) return true;
    
    std::string prefix(difficulty, '0');
    return hash.substr(0, difficulty) == prefix;
}

void Miner::printMiningProgress(unsigned long long nonce, const std::string& hash) {
    std::cout << "[MINER] Progress - Nonce: " << nonce 
              << ", Hash: " << hash.substr(0, 16) << "..." << std::endl;
}

