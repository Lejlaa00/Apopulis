#pragma once
#include "Block.h"
#include <string>

class Miner {
public:
    // Mine a new block (single-threaded for now)
    static Block mineBlock(int index, const std::string& data, 
                          const std::string& previousHash, int difficulty);
    
    // Check if hash meets difficulty requirement
    static bool hashMeetsDifficulty(const std::string& hash, int difficulty);

private:
    static void printMiningProgress(unsigned long long nonce, const std::string& hash);
};

