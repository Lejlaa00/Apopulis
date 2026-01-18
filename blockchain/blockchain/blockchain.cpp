#include "Blockchain.h"
#include <iostream>
#include <cmath>
#include <ctime>
#include <sstream>

Blockchain::Blockchain() {
    chain.push_back(createGenesisBlock());
}

Block Blockchain::createGenesisBlock() {
    Block genesis(0, "Genesis Block", "0", 2); // Start with difficulty 2 for parallel mining
    genesis.timestamp = std::time(nullptr);
    genesis.hash = genesis.calculateHash();
    genesis.miningTime = 0.0;  // Genesis block not mined
    return genesis;
}

bool Blockchain::addBlock(const Block& newBlock) {
    if (chain.empty()) {
        return false;
    }
    
    if (isValidNewBlock(newBlock, getLatestBlock())) {
        chain.push_back(newBlock);
        std::cout << "[BLOCKCHAIN] Block #" << newBlock.index << " added to chain. Hash: " 
                  << newBlock.hash.substr(0, 16) << "..." << std::endl;
        return true;
    }
    
    std::cout << "[BLOCKCHAIN] Invalid block rejected." << std::endl;
    return false;
}

bool Blockchain::isValidNewBlock(const Block& newBlock, const Block& previousBlock) const {
    // Check index
    if (previousBlock.index + 1 != newBlock.index) {
        std::cout << "[VALIDATION] Invalid index. Expected: " << (previousBlock.index + 1) 
                  << ", Got: " << newBlock.index << std::endl;
        return false;
    }
    
    // Check previous hash
    if (previousBlock.hash != newBlock.previousHash) {
        std::cout << "[VALIDATION] Invalid previous hash." << std::endl;
        return false;
    }
    
    // Check if the hash matches
    if (newBlock.calculateHash() != newBlock.hash) {
        std::cout << "[VALIDATION] Invalid hash calculation." << std::endl;
        return false;
    }
    
    // Check if hash meets difficulty requirement
    if (!hasValidHash(newBlock)) {
        std::cout << "[VALIDATION] Hash doesn't meet difficulty requirement." << std::endl;
        return false;
    }
    
    // Validate timestamp
    if (!isValidTimestamp(newBlock, previousBlock)) {
        std::cout << "[VALIDATION] Invalid timestamp." << std::endl;
        return false;
    }
    
    return true;
}

bool Blockchain::hasValidHash(const Block& block) const {
    std::string prefix(block.difficulty, '0');
    return block.hash.substr(0, block.difficulty) == prefix;
}

bool Blockchain::isValidTimestamp(const Block& newBlock, const Block& previousBlock) const {
    time_t currentTime = std::time(nullptr);
    
    // Block is valid if its timestamp is at most 1 minute greater than current time
    if (newBlock.timestamp > currentTime + 60) {
        return false;
    }
    
    // Block is valid if its timestamp is at most 1 minute less than previous block's timestamp
    if (newBlock.timestamp < previousBlock.timestamp - 60) {
        return false;
    }
    
    return true;
}

bool Blockchain::isValidChain(const std::vector<Block>& chainToValidate) const {
    if (chainToValidate.empty()) {
        return false;
    }
    
    // Check genesis block
    const Block& genesisBlock = chainToValidate[0];
    if (genesisBlock.index != 0 || genesisBlock.previousHash != "0") {
        return false;
    }
    
    // Validate each block
    for (size_t i = 1; i < chainToValidate.size(); i++) {
        if (!isValidNewBlock(chainToValidate[i], chainToValidate[i - 1])) {
            return false;
        }
    }
    
    return true;
}

bool Blockchain::replaceChain(const std::vector<Block>& newChain) {
    if (!isValidChain(newChain)) {
        std::cout << "[BLOCKCHAIN] Received chain is invalid." << std::endl;
        return false;
    }
    
    double currentDifficulty = getCumulativeDifficulty(chain);
    double newDifficulty = getCumulativeDifficulty(newChain);
    
    if (newDifficulty > currentDifficulty) {
        std::cout << "[BLOCKCHAIN] Replacing chain. New cumulative difficulty: " 
                  << newDifficulty << " > " << currentDifficulty << std::endl;
        chain = newChain;
        return true;
    }
    
    std::cout << "[BLOCKCHAIN] Received chain has lower cumulative difficulty." << std::endl;
    return false;
}

double Blockchain::getCumulativeDifficulty(const std::vector<Block>& chainToCheck) const {
    double total = 0;
    for (const auto& block : chainToCheck) {
        total += std::pow(2, block.difficulty);
    }
    return total;
}

int Blockchain::getDifficulty() const {
    return calculateDifficulty(false); // No logging for backward compatibility
}

int Blockchain::getDifficulty(int mpiRank) const {
    return calculateDifficulty(mpiRank == 0); // Only log on rank 0
}

int Blockchain::calculateDifficulty(bool enableLogging) const {
    const Block& latestBlock = getLatestBlock();
    
    // If we haven't reached the adjustment interval, keep the same difficulty
    if (latestBlock.index % DIFFICULTY_ADJUSTMENT_INTERVAL != 0 || latestBlock.index == 0) {
        return latestBlock.difficulty;
    }
    
    // Get the adjustment block
    const Block& adjustmentBlock = chain[chain.size() - DIFFICULTY_ADJUSTMENT_INTERVAL];
    
    // Calculate expected and actual MINING time (not wall-clock time)
    double expectedTime = static_cast<double>(BLOCK_GENERATION_INTERVAL) * static_cast<double>(DIFFICULTY_ADJUSTMENT_INTERVAL);
    
    // Sum actual mining time for the last DIFFICULTY_ADJUSTMENT_INTERVAL blocks
    double actualMiningTime = 0.0;
    for (size_t i = chain.size() - DIFFICULTY_ADJUSTMENT_INTERVAL; i < chain.size(); i++) {
        actualMiningTime += chain[i].miningTime;
    }
    
    // Calculate adjustment factor
    double timeFactor = actualMiningTime / expectedTime;
    
    int newDiff = adjustmentBlock.difficulty;
    
    // Adjust difficulty based on mining time (only ±1 at a time for smoother adjustment)
    if (timeFactor < 0.5) {
        // Mining was 2x faster than target - increase difficulty
        newDiff = adjustmentBlock.difficulty + 1;
        if (enableLogging) {
            std::cout << "[DIFFICULTY] Mining too fast (" << actualMiningTime << "s vs " << expectedTime 
                      << "s target). Increasing: " << adjustmentBlock.difficulty << " -> " << newDiff << std::endl;
        }
    } 
    else if (timeFactor > 2.0) {
        // Mining was 2x slower than target - decrease difficulty
        newDiff = adjustmentBlock.difficulty - 1;
        if (newDiff < 1) newDiff = 1; // Minimum difficulty of 1
        if (enableLogging) {
            std::cout << "[DIFFICULTY] Mining too slow (" << actualMiningTime << "s vs " << expectedTime 
                      << "s target). Decreasing: " << adjustmentBlock.difficulty << " -> " << newDiff << std::endl;
        }
    }
    else {
        if (enableLogging) {
            std::cout << "[DIFFICULTY] Mining time acceptable (" << actualMiningTime << "s vs " << expectedTime 
                      << "s target). Keeping difficulty at " << adjustmentBlock.difficulty << std::endl;
        }
    }
    
    return newDiff;
}

std::string Blockchain::toJson() const {
    std::stringstream ss;
    ss << "[";
    for (size_t i = 0; i < chain.size(); i++) {
        ss << chain[i].toJson();
        if (i < chain.size() - 1) {
            ss << ",";
        }
    }
    ss << "]";
    return ss.str();
}

std::vector<Block> Blockchain::chainFromJson(const std::string& json) {
    std::vector<Block> result;
    
    // Simple JSON array parsing
    size_t pos = 1; // Skip opening '['
    while (pos < json.length()) {
        size_t start = json.find('{', pos);
        if (start == std::string::npos) break;
        
        size_t end = json.find('}', start);
        if (end == std::string::npos) break;
        
        std::string blockJson = json.substr(start, end - start + 1);
        result.push_back(Block::fromJson(blockJson));
        
        pos = end + 1;
    }
    
    return result;
}
