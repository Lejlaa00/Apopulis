// Disable Microsoft's deprecation warnings for sscanf
#define _CRT_SECURE_NO_WARNINGS

#include "Block.h"
#include <openssl/evp.h>
#include <sstream>
#include <iomanip>

Block::Block(int idx, const std::string& blockData, const std::string& prevHash, int diff)
    : index(idx), data(blockData), previousHash(prevHash), difficulty(diff), nonce(0), miningTime(0.0) {
    timestamp = std::time(nullptr);
    hash = "";
}

std::string Block::sha256(const std::string& input) {
    // Use OpenSSL 3.0 EVP API (modern, recommended approach)
    unsigned char hash[EVP_MAX_MD_SIZE];
    unsigned int hashLen = 0;
    
    EVP_MD_CTX* context = EVP_MD_CTX_new();
    if (context == nullptr) {
        return "";
    }
    
    if (EVP_DigestInit_ex(context, EVP_sha256(), nullptr) != 1) {
        EVP_MD_CTX_free(context);
        return "";
    }
    
    if (EVP_DigestUpdate(context, input.c_str(), input.size()) != 1) {
        EVP_MD_CTX_free(context);
        return "";
    }
    
    if (EVP_DigestFinal_ex(context, hash, &hashLen) != 1) {
        EVP_MD_CTX_free(context);
        return "";
    }
    
    EVP_MD_CTX_free(context);
    
    // Convert to hex string
    std::stringstream ss;
    for(unsigned int i = 0; i < hashLen; i++) {
        ss << std::hex << std::setw(2) << std::setfill('0') << (int)hash[i];
    }
    return ss.str();
}

std::string Block::calculateHash() const {
    std::stringstream ss;
    ss << index << data << timestamp << previousHash << difficulty << nonce;
    return sha256(ss.str());
}

std::string Block::toJson() const {
    std::stringstream ss;
    ss << "{"
       << "\"index\":" << index << ","
       << "\"data\":\"" << data << "\","
       << "\"timestamp\":" << timestamp << ","
       << "\"previousHash\":\"" << previousHash << "\","
       << "\"difficulty\":" << difficulty << ","
       << "\"nonce\":" << nonce << ","
       << "\"hash\":\"" << hash << "\","
       << "\"miningTime\":" << miningTime
       << "}";
    return ss.str();
}

Block Block::fromJson(const std::string& json) {
    // Simple JSON parsing (for production, use a proper JSON library)
    int idx = 0, diff = 0;
    time_t ts = 0;
    unsigned long long n = 0;
    double mTime = 0.0;
    std::string blockData, prevHash, blockHash;
    
    // Extract values (simple parsing)
    size_t pos = json.find("\"index\":");
    if (pos != std::string::npos) {
        sscanf(json.c_str() + pos + 8, "%d", &idx);
    }
    
    pos = json.find("\"data\":\"");
    if (pos != std::string::npos) {
        size_t start = pos + 8;
        size_t end = json.find("\"", start);
        blockData = json.substr(start, end - start);
    }
    
    pos = json.find("\"timestamp\":");
    if (pos != std::string::npos) {
        sscanf(json.c_str() + pos + 12, "%lld", &ts);
    }
    
    pos = json.find("\"previousHash\":\"");
    if (pos != std::string::npos) {
        size_t start = pos + 16;
        size_t end = json.find("\"", start);
        prevHash = json.substr(start, end - start);
    }
    
    pos = json.find("\"difficulty\":");
    if (pos != std::string::npos) {
        sscanf(json.c_str() + pos + 13, "%d", &diff);
    }
    
    pos = json.find("\"nonce\":");
    if (pos != std::string::npos) {
        sscanf(json.c_str() + pos + 8, "%llu", &n);
    }
    
    pos = json.find("\"hash\":\"");
    if (pos != std::string::npos) {
        size_t start = pos + 8;
        size_t end = json.find("\"", start);
        blockHash = json.substr(start, end - start);
    }
    
    pos = json.find("\"miningTime\":");
    if (pos != std::string::npos) {
        sscanf(json.c_str() + pos + 13, "%lf", &mTime);
    }
    
    Block block(idx, blockData, prevHash, diff);
    block.timestamp = ts;
    block.nonce = n;
    block.hash = blockHash;
    block.miningTime = mTime;
    return block;
}

