# ETHWalletChecker
Its a ETH Wallet Checker. it checks if there's any balance in the 24 key seed provided by a user and uses the different api's to check the data on the mainnet

This Java program checks the balance of Ethereum wallets generated from a seed phrase by using the Web3j library. The program generates all possible permutations of the seed phrase and checks the balance of each generated wallet asynchronously. The program outputs the seed phrases with positive balances to a file called "positive_balance.txt" and the seed phrases with zero balances to a file called "zero_balance.txt".

Prerequisites
Java 8 or later
IntelliJ IDEA or another Java IDE
Web3j library
OkHttp3 library
Installation
Clone the repository
Open the project in IntelliJ IDEA
Add the Web3j and OkHttp3 libraries to the project classpath
Replace the placeholder values in the code with your own seed phrase and API URLs
Run the program
Usage
When you run the program, it will generate all possible permutations of the seed phrase and check the balance of each generated wallet asynchronously. The program outputs the seed phrases with positive balances to a file called "positive_balance.txt" and the seed phrases with zero balances to a file called "zero_balance.txt".

Note: The program will make API calls to the specified URLs to check the balance of the wallets. Make sure that you have access to the APIs and replace the placeholder URLs in the code with your own API URLs.

License
This software is licensed under the Business Source License (BSL), Version 1.1. If you use this software, you are required to pay a fee after a certain period of time. Please see the [license agreement](https://www.businesssourcelicense.com/license/bsl1.1.txt) for more information.
