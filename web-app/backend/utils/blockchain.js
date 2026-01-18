const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const BLOCKCHAIN_FILE = path.join(__dirname, '../data/blockchain.json');
const DIFFICULTY = 3; // Number of leading zeros required (3 = ~1-2 seconds, 4 = ~10-20 seconds)
const BLOCK_GENERATION_INTERVAL = 10; // Target: 10 seconds per block
const DIFFICULTY_ADJUSTMENT_INTERVAL = 2; // Adjust difficulty every 2 blocks

/**
 * Simple Blockchain Implementation for Educational Purposes
 * Demonstrates: Hashing, Proof-of-Work, Chain Validation, Difficulty Adjustment
 */

class Block {
    constructor(index, data, previousHash, difficulty = DIFFICULTY) {
        this.index = index;
        this.data = data;
        this.timestamp = Date.now();
        this.previousHash = previousHash;
        this.difficulty = difficulty;
        this.nonce = 0;
        this.hash = '';
    }

    /**
     * Calculate SHA-256 hash of the block
     */
    calculateHash() {
        const dataString = `${this.index}${this.data}${this.timestamp}${this.previousHash}${this.difficulty}${this.nonce}`;
        return crypto.createHash('sha256').update(dataString).digest('hex');
    }

    /**
     * Check if hash meets the difficulty requirement (starts with N zeros)
     */
    hashMeetsDifficulty(hash) {
        const prefix = '0'.repeat(this.difficulty);
        return hash.startsWith(prefix);
    }

    /**
     * Mine the block - find a nonce that produces a hash meeting the difficulty
     * This is the "proof-of-work" - computationally expensive to find, easy to verify
     */
    mine() {
        console.log(`[Blockchain] Mining block #${this.index} with difficulty ${this.difficulty}...`);
        const startTime = Date.now();
        
        while (!this.hashMeetsDifficulty(this.hash)) {
            this.nonce++;
            this.hash = this.calculateHash();
            
            // Log progress every 10000 attempts
            if (this.nonce % 10000 === 0) {
                process.stdout.write(`\r[Blockchain] Attempts: ${this.nonce.toLocaleString()}...`);
            }
        }
        
        const miningTime = ((Date.now() - startTime) / 1000).toFixed(2);
        console.log(`\n[Blockchain] ✓ Block mined in ${miningTime}s (nonce: ${this.nonce.toLocaleString()})`);
        return this;
    }

    /**
     * Validate block structure and hash
     */
    isValid(previousBlock) {
        // Check hash calculation
        if (this.calculateHash() !== this.hash) {
            console.error('[Blockchain] Invalid: Hash calculation mismatch');
            return false;
        }

        // Check difficulty requirement
        if (!this.hashMeetsDifficulty(this.hash)) {
            console.error('[Blockchain] Invalid: Hash does not meet difficulty requirement');
            return false;
        }

        // Check previous hash link (except for genesis block)
        if (previousBlock && this.previousHash !== previousBlock.hash) {
            console.error('[Blockchain] Invalid: Previous hash mismatch');
            return false;
        }

        // Check index continuity
        if (previousBlock && this.index !== previousBlock.index + 1) {
            console.error('[Blockchain] Invalid: Index not continuous');
            return false;
        }

        return true;
    }
}

class Blockchain {
    constructor() {
        this.chain = [];
        this.loadChain();
        
        // Create genesis block if chain is empty
        if (this.chain.length === 0) {
            this.createGenesisBlock();
        }
    }

    /**
     * Create the first block in the chain (Genesis Block)
     */
    createGenesisBlock() {
        const genesis = new Block(0, 'Genesis Block - Apopulis News Blockchain', '0', DIFFICULTY);
        genesis.hash = genesis.calculateHash();
        // Genesis block doesn't need mining (or use difficulty 0)
        genesis.difficulty = 0;
        this.chain.push(genesis);
        this.saveChain();
        console.log('[Blockchain] Genesis block created');
    }

    /**
     * Get the latest block in the chain
     */
    getLatestBlock() {
        return this.chain[this.chain.length - 1];
    }

    /**
     * Calculate current difficulty based on block generation time
     * Adjusts difficulty to maintain ~10 seconds per block
     */
    getDifficulty() {
        if (this.chain.length < DIFFICULTY_ADJUSTMENT_INTERVAL + 1) {
            return DIFFICULTY;
        }

        const latestBlock = this.getLatestBlock();
        const previousAdjustmentBlock = this.chain[this.chain.length - DIFFICULTY_ADJUSTMENT_INTERVAL - 1];
        
        const timeExpected = BLOCK_GENERATION_INTERVAL * DIFFICULTY_ADJUSTMENT_INTERVAL;
        const timeTaken = (latestBlock.timestamp - previousAdjustmentBlock.timestamp) / 1000;
        
        const timeFactor = timeTaken / timeExpected;
        
        let newDifficulty = previousAdjustmentBlock.difficulty;
        
        if (timeFactor < 0.4) {
            newDifficulty += 2; // Very fast - increase difficulty significantly
        } else if (timeFactor < 0.75) {
            newDifficulty += 1; // Fast - increase difficulty
        } else if (timeFactor > 2.5) {
            newDifficulty -= 2; // Very slow - decrease difficulty significantly
        } else if (timeFactor > 1.33) {
            newDifficulty -= 1; // Slow - decrease difficulty
        }
        
        // Keep difficulty between 1 and 6 for reasonable mining times
        return Math.max(1, Math.min(6, newDifficulty));
    }

    /**
     * Add a new block to the blockchain
     * @param {string} data - The data to store in the block (e.g., news item ID)
     * @returns {Block} The newly mined block
     */
    addBlock(data) {
        const previousBlock = this.getLatestBlock();
        const difficulty = this.getDifficulty();
        
        const newBlock = new Block(
            previousBlock.index + 1,
            data,
            previousBlock.hash,
            difficulty
        );

        // Mine the block (proof-of-work)
        newBlock.mine();

        // Validate before adding
        if (newBlock.isValid(previousBlock)) {
            this.chain.push(newBlock);
            this.saveChain();
            console.log(`[Blockchain] Block #${newBlock.index} added to chain`);
            return newBlock;
        } else {
            throw new Error('Invalid block cannot be added to chain');
        }
    }

    /**
     * Validate the entire blockchain
     */
    isValidChain() {
        // Check genesis block
        const genesis = this.chain[0];
        if (genesis.index !== 0 || genesis.previousHash !== '0') {
            console.error('[Blockchain] Invalid genesis block');
            return false;
        }

        // Check each block
        for (let i = 1; i < this.chain.length; i++) {
            const currentBlock = this.chain[i];
            const previousBlock = this.chain[i - 1];

            if (!currentBlock.isValid(previousBlock)) {
                console.error(`[Blockchain] Invalid block at index ${i}`);
                return false;
            }
        }

        return true;
    }

    /**
     * Get blockchain statistics
     */
    getStats() {
        return {
            length: this.chain.length,
            latestBlock: {
                index: this.getLatestBlock().index,
                hash: this.getLatestBlock().hash.substring(0, 16) + '...',
                difficulty: this.getLatestBlock().difficulty,
                timestamp: new Date(this.getLatestBlock().timestamp).toISOString()
            },
            currentDifficulty: this.getDifficulty(),
            isValid: this.isValidChain()
        };
    }

    /**
     * Load blockchain from JSON file
     */
    loadChain() {
        try {
            if (fs.existsSync(BLOCKCHAIN_FILE)) {
                const data = fs.readFileSync(BLOCKCHAIN_FILE, 'utf8');
                this.chain = JSON.parse(data);
                console.log(`[Blockchain] Loaded ${this.chain.length} blocks from file`);
            }
        } catch (error) {
            console.error('[Blockchain] Error loading chain:', error.message);
            this.chain = [];
        }
    }

    /**
     * Save blockchain to JSON file
     */
    saveChain() {
        try {
            // Ensure data directory exists
            const dataDir = path.dirname(BLOCKCHAIN_FILE);
            if (!fs.existsSync(dataDir)) {
                fs.mkdirSync(dataDir, { recursive: true });
            }

            fs.writeFileSync(BLOCKCHAIN_FILE, JSON.stringify(this.chain, null, 2));
        } catch (error) {
            console.error('[Blockchain] Error saving chain:', error.message);
        }
    }
}

// Export singleton instance
const blockchain = new Blockchain();

module.exports = blockchain;
