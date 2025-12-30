#include "Block.h"
#include "Blockchain.h"
#include "Miner.h"
#include <iostream>
#include <string>
#include <ctime>
#include <sstream>
#include <thread>
#include <atomic>
#include <mutex>

// HTTP server support
#define CPPHTTPLIB_OPENSSL_SUPPORT
#include "httplib.h"

// Global mutex for blockchain access
std::mutex blockchainMutex;
std::atomic<bool> httpServerRunning(false);

void printMenu() {
    std::cout << "\n========== BLOCKCHAIN NODE ==========" << std::endl;
    std::cout << "1. Mine new block" << std::endl;
    std::cout << "2. Show blockchain" << std::endl;
    std::cout << "3. Validate blockchain" << std::endl;
    std::cout << "4. Show chain info" << std::endl;
    std::cout << "5. Send block to peer" << std::endl;
    std::cout << "6. Sync chain from peer" << std::endl;
    std::cout << "7. Exit" << std::endl;
    std::cout << "=====================================" << std::endl;
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

void startHttpServer(Blockchain& blockchain, int port) {
    httplib::Server svr;
    
    // GET /chain - Get the entire blockchain
    svr.Get("/chain", [&blockchain](const httplib::Request& req, httplib::Response& res) {
        std::lock_guard<std::mutex> lock(blockchainMutex);
        res.set_content(blockchain.toJson(), "application/json");
        std::cout << "[HTTP] Sent blockchain to peer" << std::endl;
    });
    
    // POST /block - Receive a new block from peer
    svr.Post("/block", [&blockchain](const httplib::Request& req, httplib::Response& res) {
        std::lock_guard<std::mutex> lock(blockchainMutex);
        try {
            Block receivedBlock = Block::fromJson(req.body);
            std::cout << "[HTTP] Received block #" << receivedBlock.index << " from peer" << std::endl;
            
            if (blockchain.addBlock(receivedBlock)) {
                res.set_content("{\"status\":\"success\"}", "application/json");
                res.status = 200;
                std::cout << "[HTTP] Block accepted and added to chain" << std::endl;
            } else {
                res.set_content("{\"status\":\"invalid block\"}", "application/json");
                res.status = 400;
                std::cout << "[HTTP] Block rejected - validation failed" << std::endl;
            }
        } catch (...) {
            res.set_content("{\"status\":\"error\"}", "application/json");
            res.status = 500;
            std::cout << "[HTTP] Error processing block" << std::endl;
        }
    });
    
    // POST /chain - Receive an entire chain from peer
    svr.Post("/chain", [&blockchain](const httplib::Request& req, httplib::Response& res) {
        std::lock_guard<std::mutex> lock(blockchainMutex);
        try {
            std::vector<Block> receivedChain = Blockchain::chainFromJson(req.body);
            std::cout << "[HTTP] Received chain with " << receivedChain.size() << " blocks from peer" << std::endl;
            
            if (blockchain.replaceChain(receivedChain)) {
                res.set_content("{\"status\":\"chain replaced\"}", "application/json");
                res.status = 200;
                std::cout << "[HTTP] Chain replaced with received chain" << std::endl;
            } else {
                res.set_content("{\"status\":\"chain not replaced\"}", "application/json");
                res.status = 400;
                std::cout << "[HTTP] Chain rejected - validation failed or lower difficulty" << std::endl;
            }
        } catch (...) {
            res.set_content("{\"status\":\"error\"}", "application/json");
            res.status = 500;
            std::cout << "[HTTP] Error processing chain" << std::endl;
        }
    });
    
    // GET /info - Get node info
    svr.Get("/info", [&blockchain, port](const httplib::Request& req, httplib::Response& res) {
        std::lock_guard<std::mutex> lock(blockchainMutex);
        std::stringstream ss;
        ss << "{"
           << "\"port\":" << port << ","
           << "\"chainLength\":" << blockchain.getChain().size() << ","
           << "\"difficulty\":" << blockchain.getDifficulty() << ","
           << "\"cumulativeDifficulty\":" << blockchain.getCumulativeDifficulty(blockchain.getChain())
           << "}";
        res.set_content(ss.str(), "application/json");
    });
    
    std::cout << "[HTTP] Server started on http://localhost:" << port << std::endl;
    std::cout << "[HTTP] Endpoints:" << std::endl;
    std::cout << "  GET  /chain - Get blockchain" << std::endl;
    std::cout << "  POST /block - Submit a block" << std::endl;
    std::cout << "  POST /chain - Submit a chain" << std::endl;
    std::cout << "  GET  /info  - Get node info" << std::endl;
    
    httpServerRunning = true;
    svr.listen("0.0.0.0", port);
}

void sendBlockToPeer(const Block& block, const std::string& peerUrl) {
    try {
        httplib::Client cli(peerUrl.c_str());
        auto res = cli.Post("/block", block.toJson(), "application/json");
        
        if (res && res->status == 200) {
            std::cout << "[PEER] Block #" << block.index << " sent successfully to " << peerUrl << std::endl;
        } else {
            std::cout << "[PEER] Failed to send block to " << peerUrl << std::endl;
        }
    } catch (...) {
        std::cout << "[PEER] Error connecting to " << peerUrl << std::endl;
    }
}

void syncChainFromPeer(Blockchain& blockchain, const std::string& peerUrl) {
    try {
        httplib::Client cli(peerUrl.c_str());
        auto res = cli.Get("/chain");
        
        if (res && res->status == 200) {
            std::lock_guard<std::mutex> lock(blockchainMutex);
            std::vector<Block> receivedChain = Blockchain::chainFromJson(res->body);
            std::cout << "[PEER] Received chain with " << receivedChain.size() << " blocks from " << peerUrl << std::endl;
            
            if (blockchain.replaceChain(receivedChain)) {
                std::cout << "[PEER] Chain synchronized successfully!" << std::endl;
            } else {
                std::cout << "[PEER] Current chain is better, keeping it" << std::endl;
            }
        } else {
            std::cout << "[PEER] Failed to get chain from " << peerUrl << std::endl;
        }
    } catch (...) {
        std::cout << "[PEER] Error connecting to " << peerUrl << std::endl;
    }
}

int main(int argc, char* argv[]) {
    // Parse command line arguments
    int port = 8080;
    std::string peerUrl = "";
    
    for (int i = 1; i < argc; i++) {
        std::string arg = argv[i];
        if (arg == "--port" && i + 1 < argc) {
            port = std::stoi(argv[++i]);
        } else if (arg == "--peer" && i + 1 < argc) {
            peerUrl = argv[++i];
        }
    }
    
    std::cout << "========================================" << std::endl;
    std::cout << "   BLOCKCHAIN NODE - C++ Implementation" << std::endl;
    std::cout << "========================================" << std::endl;
    std::cout << "Port: " << port << std::endl;
    if (!peerUrl.empty()) {
        std::cout << "Peer: " << peerUrl << std::endl;
    }
    std::cout << "========================================" << std::endl;
    
    // Initialize blockchain
    Blockchain blockchain;
    
    std::cout << "\n[INIT] Genesis block created." << std::endl;
    std::cout << "[INIT] Blockchain initialized with " << blockchain.getChain().size() << " block(s)." << std::endl;
    
    // Start HTTP server in background thread
    std::thread httpThread(startHttpServer, std::ref(blockchain), port);
    httpThread.detach();
    
    // Give HTTP server time to start
    std::this_thread::sleep_for(std::chrono::milliseconds(500));
    
    // Sync with peer if provided
    if (!peerUrl.empty()) {
        std::cout << "\n[INIT] Syncing with peer..." << std::endl;
        syncChainFromPeer(blockchain, peerUrl);
    }
    
    // Main loop
    bool running = true;
    while (running) {
        printMenu();
        
        int choice;
        std::cin >> choice;
        
        if (std::cin.fail()) {
            std::cin.clear();
            std::cin.ignore(10000, '\n');
            std::cout << "Invalid input. Please enter a number." << std::endl;
            continue;
        }
        
        switch (choice) {
            case 1: {
                // Mine new block
                std::cout << "Enter block data: ";
                std::cin.ignore();
                std::string data;
                std::getline(std::cin, data);
                
                Block* newBlock = nullptr;
                {
                    std::lock_guard<std::mutex> lock(blockchainMutex);
                    const Block& previousBlock = blockchain.getLatestBlock();
                    int difficulty = blockchain.getDifficulty();
                    
                    std::cout << "\n[INFO] Mining block #" << (previousBlock.index + 1) 
                             << " with difficulty " << difficulty << std::endl;
                    
                    Block minedBlock = Miner::mineBlock(
                        previousBlock.index + 1,
                        data,
                        previousBlock.hash,
                        difficulty
                    );
                    
                    if (blockchain.addBlock(minedBlock)) {
                        std::cout << "[SUCCESS] Block added to blockchain!" << std::endl;
                        newBlock = new Block(minedBlock);
                    } else {
                        std::cout << "[ERROR] Failed to add block to blockchain!" << std::endl;
                    }
                }
                
                // Send to peer if configured
                if (!peerUrl.empty() && newBlock != nullptr) {
                    std::cout << "[INFO] Sending block to peer..." << std::endl;
                    sendBlockToPeer(*newBlock, peerUrl);
                    delete newBlock;
                }
                break;
            }
            
            case 2: {
                // Show blockchain
                std::lock_guard<std::mutex> lock(blockchainMutex);
                printChain(blockchain);
                break;
            }
            
            case 3: {
                // Validate blockchain
                std::lock_guard<std::mutex> lock(blockchainMutex);
                std::cout << "\n[VALIDATION] Validating blockchain..." << std::endl;
                if (blockchain.isValidChain(blockchain.getChain())) {
                    std::cout << "[SUCCESS] Blockchain is valid!" << std::endl;
                } else {
                    std::cout << "[ERROR] Blockchain is invalid!" << std::endl;
                }
                break;
            }
            
            case 4: {
                // Show chain info
                std::lock_guard<std::mutex> lock(blockchainMutex);
                printChainInfo(blockchain);
                break;
            }
            
            case 5: {
                // Send block to peer
                if (peerUrl.empty()) {
                    std::cout << "No peer configured. Start with --peer <url>" << std::endl;
                } else {
                    int blockIndex;
                    std::cout << "Enter block index to send: ";
                    std::cin >> blockIndex;
                    
                    std::lock_guard<std::mutex> lock(blockchainMutex);
                    if (blockIndex >= 0 && blockIndex < (int)blockchain.getChain().size()) {
                        sendBlockToPeer(blockchain.getChain()[blockIndex], peerUrl);
                    } else {
                        std::cout << "Invalid block index!" << std::endl;
                    }
                }
                break;
            }
            
            case 6: {
                // Sync chain from peer
                if (peerUrl.empty()) {
                    std::cout << "Enter peer URL (e.g., http://localhost:8081): ";
                    std::cin.ignore();
                    std::getline(std::cin, peerUrl);
                }
                syncChainFromPeer(blockchain, peerUrl);
                break;
            }
            
            case 7: {
                // Exit
                std::cout << "\nExiting..." << std::endl;
                running = false;
                break;
            }
            
            default: {
                std::cout << "Invalid choice. Please try again." << std::endl;
                break;
            }
        }
    }
    
    return 0;
}
