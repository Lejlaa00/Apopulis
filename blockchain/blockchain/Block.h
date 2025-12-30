#pragma once
#include <string>
#include <ctime>
#include <sstream>
#include <iomanip>

class Block {
public:
    int index;
    std::string data;
    time_t timestamp;
    std::string previousHash;
    int difficulty;
    unsigned long long nonce;
    std::string hash;

    Block(int idx, const std::string& blockData, const std::string& prevHash, int diff);
    
    // Calculate hash of the block
    std::string calculateHash() const;
    
    // Convert block to JSON string
    std::string toJson() const;
    
    // Create block from JSON string
    static Block fromJson(const std::string& json);

private:
    static std::string sha256(const std::string& input);
};

