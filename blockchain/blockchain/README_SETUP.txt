========================================
BLOCKCHAIN C++ PROJECT - SETUP GUIDE
========================================

This is a C++ blockchain implementation with the following features:
- Block structure with all required fields
- Blockchain validation
- Mining algorithm (proof-of-work)
- Dynamic difficulty adjustment
- Timestamp validation
- Cumulative difficulty for chain selection
- HTTP server support (to be added)

========================================
REQUIRED LIBRARIES
========================================

1. OpenSSL (for SHA-256 hashing)
   - Windows: Download from https://slproweb.com/products/Win32OpenSSL.html
   - Install "Win64 OpenSSL v3.x.x" (or Win32 if you have 32-bit system)
   - Common install path: C:\Program Files\OpenSSL-Win64\

2. cpp-httplib (header-only library for HTTP server)
   - Download httplib.h from: https://raw.githubusercontent.com/yhirose/cpp-httplib/master/httplib.h
   - Place it in the blockchain folder (replace the placeholder httplib.h)

========================================
VISUAL STUDIO SETUP
========================================

1. Configure OpenSSL in Visual Studio:
   
   a) Right-click project -> Properties
   
   b) Under "C/C++" -> "General" -> "Additional Include Directories", add:
      C:\Program Files\OpenSSL-Win64\include
   
   c) Under "Linker" -> "General" -> "Additional Library Directories", add:
      C:\Program Files\OpenSSL-Win64\lib
   
   d) Under "Linker" -> "Input" -> "Additional Dependencies", add:
      libcrypto.lib
      libssl.lib
      ws2_32.lib
      crypt32.lib
   
   e) Under "C/C++" -> "Language" -> "C++ Language Standard", select:
      ISO C++17 Standard (/std:c++17) or later

2. Add source files to project:
   - Block.h, Block.cpp
   - Blockchain.h, Blockchain.cpp
   - Miner.h, Miner.cpp
   - main.cpp
   
   Right-click on "Source Files" -> Add -> Existing Item -> select .cpp files
   Right-click on "Header Files" -> Add -> Existing Item -> select .h files

3. Set main.cpp as the startup file (it should be by default)

4. Remove or exclude blockchain.cpp from build (it's not needed)

========================================
BUILDING THE PROJECT
========================================

1. Build -> Build Solution (or press Ctrl+Shift+B)
2. If you get OpenSSL errors, verify the paths in project properties
3. Make sure you're building for x64 if you installed 64-bit OpenSSL

========================================
TESTING THE PROGRAM
========================================

Basic Test (Single Node):

1. Run the program (F5 or Ctrl+F5)

2. You'll see a menu:
   1. Mine new block
   2. Show blockchain
   3. Validate blockchain
   4. Show chain info
   5. Exit

3. Test Case 1 - Mine a single block:
   - Choose option 1
   - Enter some data (e.g., "Test transaction 1")
   - Watch the mining process
   - Block will be mined with difficulty 0

4. Test Case 2 - Show blockchain:
   - Choose option 2
   - You'll see the genesis block and any mined blocks

5. Test Case 3 - Mine multiple blocks (test difficulty adjustment):
   - Mine blocks by choosing option 1 repeatedly
   - Enter different data for each block
   - After 10 blocks, difficulty should adjust based on time
   - Use option 4 to see current difficulty

6. Test Case 4 - Validate chain:
   - Choose option 3
   - Should show "Blockchain is valid!"

Expected Output Example:

========== BLOCKCHAIN NODE ==========
1. Mine new block
2. Show blockchain
3. Validate blockchain
4. Show chain info
5. Exit
=====================================
Enter choice: 1
Enter block data: First transaction
[MINER] Starting to mine block #1 with difficulty 0
[MINER] Block #1 mined!
  Nonce: 0
  Hash: a1b2c3d4e5f6...
  Time: 0.001 seconds
  Hash rate: 1000 H/s
[BLOCKCHAIN] Block #1 added to chain.

========================================
TESTING DIFFICULTY ADJUSTMENT
========================================

The difficulty adjusts every 10 blocks:
- If blocks are mined too fast (< 5 seconds per block on average), difficulty increases
- If blocks are mined too slow (> 20 seconds per block on average), difficulty decreases
- Target time: 10 seconds per block

To test:
1. Mine 10 blocks quickly
2. Check difficulty with option 4
3. Continue mining to see difficulty changes

========================================
NEXT STEPS (Multi-threading & HTTP)
========================================

After verifying the basic functionality works:

1. Download and add httplib.h
2. Uncomment HTTP server code in main.cpp
3. Implement multi-threading in Miner.cpp
4. Test with multiple nodes on different ports

========================================
TROUBLESHOOTING
========================================

Error: "Cannot open include file 'openssl/sha.h'"
- Solution: Add OpenSSL include directory to project properties

Error: "Unresolved external symbol SHA256"
- Solution: Add OpenSSL library directory and libcrypto.lib to linker settings

Error: "C++ standard too old"
- Solution: Set C++ language standard to C++17 or later

Program crashes during mining:
- Check that all validation logic is working
- Verify hash calculation is correct
- Use option 3 to validate chain

========================================

