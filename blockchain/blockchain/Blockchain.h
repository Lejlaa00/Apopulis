#pragma once
#include "Block.h"
#include <vector>
#include <string>

class Blockchain {
public:
    static const int BLOCK_GENERATION_INTERVAL = 10; // seconds
    static const int DIFFICULTY_ADJUSTMENT_INTERVAL = 10; // blocks
    
    Blockchain();
    
    // Get the chain
    const std::vector<Block>& getChain() const { return chain; }
    
    // Get latest block
    const Block& getLatestBlock() const { return chain.back(); }
    
    // Add a new block to the chain
    bool addBlock(const Block& newBlock);
    
    // Validate a single block
    bool isValidNewBlock(const Block& newBlock, const Block& previousBlock) const;
    
    // Validate entire chain
    bool isValidChain(const std::vector<Block>& chainToValidate) const;
    
    // Replace chain if the new one is valid and has higher cumulative difficulty
    bool replaceChain(const std::vector<Block>& newChain);
    
    // Get current difficulty
    int getDifficulty() const;
    
    // Calculate cumulative difficulty
    double getCumulativeDifficulty(const std::vector<Block>& chain) const;
    
    // Convert chain to JSON
    std::string toJson() const;
    
    // Create chain from JSON
    static std::vector<Block> chainFromJson(const std::string& json);

private:
    std::vector<Block> chain;
    
    Block createGenesisBlock();
    int calculateDifficulty() const;
    bool isValidTimestamp(const Block& newBlock, const Block& previousBlock) const;
    bool hasValidHash(const Block& block) const;
};

