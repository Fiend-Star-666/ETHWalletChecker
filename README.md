# 🚀 ETHWalletChecker 💼

Welcome to the ETHWalletChecker! This is your friendly neighborhood Ethereum wallet balance checker. It's like having a personal assistant for your Ethereum wallets, but without the coffee runs.

## 🛠 Tech Stack

- **Java**: The backbone of our checker, providing the structure and logic.
- **Web3j**: Our connection to the Ethereum network.
- **OkHttp3**: The messenger that communicates with the APIs.

## 🌐 Project Overview

ETHWalletChecker is like a detective for your Ethereum wallets. It checks if there's any balance in the 24 key seed provided by a user. It uses different APIs to check the data on the mainnet. The program generates all possible permutations of the seed phrase and checks the balance of each generated wallet asynchronously. 

The magic lies in its ability to check all possible combinations of the seed phrase in parallel. This is achieved by using an algorithm that generates all permutations of the seed phrase, and then checking each permutation's balance asynchronously. This means that the program can check multiple wallets at the same time, greatly improving the efficiency of the balance checking process.

## 📝 Usage

When you run the program, it will generate all possible permutations of the seed phrase and check the balance of each generated wallet asynchronously. The program outputs the seed phrases with positive balances to a file called "positive_balance.txt" and the seed phrases with zero balances to a file called "zero_balance.txt".

## 🚧 Disclaimer

This code is provided as-is for educational purposes. Please use responsibly and ensure any commercial use complies with appropriate licenses and permissions.

That's it! We hope you enjoy using our ETHWalletChecker as much as we enjoyed building it. Happy checking! 💼🔍
