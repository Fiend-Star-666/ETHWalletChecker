import okhttp3.OkHttpClient;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.exceptions.ClientConnectionException;
import org.web3j.protocol.http.HttpService;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EthWalletBalanceChecker {
    private static final Logger logger = LoggerFactory.getLogger(EthWalletBalanceChecker.class);
    private static final int HTTP_TIMEOUT = 30000;
    private static final List<String> API_URLS = Arrays.asList(
            "add your api url here",
            "add your api url here"
    );
    private static final int THREADS = Runtime.getRuntime().availableProcessors()*2;

    private static final String POSITIVE_BALANCE_FILE = "positive_balance.txt";
    private static final String ZERO_BALANCE_FILE = "zero_balance.txt";
    private static final AtomicInteger API_URL_INDEX = new AtomicInteger(0);

    public static void main(String[] args) {

        logger.info("THREADS CREATEDddd" + THREADS);
        logger.info("Starting EthWalletBalanceChecker program...");

        List<String> disorderedSeedPhrase = Arrays.asList("add your seed phrase here".split(" "));
        logger.info("PHRASE CREATED");
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        logger.info("HTTP CLIENT CREATED");
        Web3j web3j = getWeb3jClient(httpClient);
        logger.info("WEB3J CLIENT CREATED");

        Set<String> usedSeedPhrases = new HashSet<>();
        usedSeedPhrases.addAll(loadSeedPhrases(POSITIVE_BALANCE_FILE));
        usedSeedPhrases.addAll(loadSeedPhrases(ZERO_BALANCE_FILE));
       // logger.info("usedSeedPhrases"+usedSeedPhrases.toString());
        logger.info("SEED PHRASES LOADED");

        AtomicInteger tasksCount = new AtomicInteger(0);

        // Use a fixed-size thread pool
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        // Limit the number of active CompletableFuture instances
        Semaphore semaphore = new Semaphore(THREADS * 2);
        logger.info("SEMAPHORE CREATED");
        Iterator<List<String>> permutationIterator = generatePermutations(disorderedSeedPhrase).iterator();

        Set<String> checkedSeedPhrases = new HashSet<>();

        while (permutationIterator.hasNext()) {
            List<String> seedPhrase = permutationIterator.next();
            logger.info("SEED PHRASE CREATED");

            String seedPhraseString = String.join(" ", seedPhrase);

            if (checkedSeedPhrases.contains(seedPhraseString)) {
                logger.info("Seed phrase already checked: {}", seedPhraseString);
                continue;
            }

            try {
                if (!isValidSeedPhrase(seedPhrase)) {
                    logger.info("Invalid seed phrase: {}", seedPhraseString);
                    Collections.shuffle(seedPhrase);
                    logger.info("SEED PHRASE SHUFFLED");
                    permutationIterator = generatePermutations(seedPhrase).iterator();
                    logger.info("PERMUTATIONS GENERATED");
                    continue;
                }
                logger.info("SEED PHRASE VALIDATED");
                semaphore.acquire();
                CompletableFuture<Void> future = checkWalletBalanceAsync(web3j, httpClient, seedPhrase, usedSeedPhrases, tasksCount, executor);

                future.whenComplete((r, e) -> semaphore.release());
                logger.info("FUTURE CREATED");

                checkedSeedPhrases.add(seedPhraseString);
            } catch (InterruptedException e) {
                logger.info("Error acquiring semaphore", e);
            } catch (Exception e) {
                logger.info("Error processing seed phrase: {}", seedPhraseString, e);
            }
        }

        executor.shutdown();
        try {
            logger.info("Waiting for executor termination...");
            boolean terminated = executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            if (terminated) {
                logger.info("Executor terminated successfully.");
            } else {
                logger.info("Executor termination failed.");
            }
        } catch (InterruptedException e) {
            logger.info("Error waiting for executor termination", e);
        }


        logger.info("All tasks completed.");
        logger.info("Total tasks processed: {}", tasksCount.get());
    }

    private static Web3j getWeb3jClient(OkHttpClient httpClient) {
        String apiUrl = API_URLS.get(API_URL_INDEX.get());
        logger.info("Switching to API URL: {}", apiUrl);
        return Web3j.build(new HttpService(apiUrl, httpClient, false));
    }

    public static Iterable<List<String>> generatePermutations(List<String> disorderedSeedPhrase) {
        logger.info("GENERATING PERMUTATIONS");
        return () -> new Iterator<>() {
            private final int n = disorderedSeedPhrase.size();
            private final int[] p;
            private final int[] d;

            {
                p = new int[n + 1];
                d = new int[n + 1];
                for (int i = 0; i <= n; i++) {
                    p[i] = i;
                    d[i] = i < n ? 1 : 0;
                }
            }

            @Override
            public boolean hasNext() {
                return p[n] != 0;
            }

            @Override
            public List<String> next() {
                List<String> result = new ArrayList<>(n);
                for (int i = 1; i <= n; i++) {
                    result.add(disorderedSeedPhrase.get(p[i] - 1));
                }

                int i = n - 1;
                while (i > 0 && p[i] + d[i] > n || p[i] + d[i] < 1) {
                    d[i] = -d[i];
                    i--;
                }

                if (i == 0) {
                    p[n] = 0;
                } else {
                    int j = p[i] + d[i];
                    int temp = p[j];
                    p[j] = p[i];
                    p[i] = temp;
                }
                logger.info("PERMUTATIONS GENERATED");
                return result;
            }
        };

    }


    private static CompletableFuture<Void> checkWalletBalanceAsync(Web3j web3j, OkHttpClient httpClient, List<String> seedPhrase, Set<String> usedSeedPhrases, AtomicInteger tasksCount, ExecutorService executor) {
        {
            return CompletableFuture.runAsync(() -> {
                try {
                    logger.info("Checking wallet balance for seed phrase: {}", String.join(" ", seedPhrase));
                    tasksCount.incrementAndGet();
                    logger.info("TASKS COUNT INCREMENTED");
                    String seedPhraseString = String.join(" ", seedPhrase);

                    if (usedSeedPhrases.contains(seedPhraseString)) {
                        logger.info("Seed phrase already used: {}", seedPhraseString);
                        return;
                    }

                    byte[] seed = MnemonicCode.toSeed(seedPhrase, "");
                    Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(seed);
                    logger.info("MASTER KEY PAIR GENERATED");
                    // Define the derivation path manually
                    int[] derivationPath = new int[]{44 | 0x80000000, 60 | 0x80000000, 0x80000000, 0, 0};

                    Bip32ECKeyPair derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, derivationPath);

                    Credentials credentials = Credentials.create(derivedKeyPair);

                    EthGetBalance balance = web3j.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();

                    BigInteger wei = balance.getBalance();

                    EthGetTransactionCount transactionCountResponse = web3j.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();

                    BigInteger transactionCount = transactionCountResponse.getTransactionCount();

                    // Check if the wallet exists (balance > 0 or transaction count > 0)
                    if (wei.compareTo(BigInteger.ZERO) > 0 || transactionCount.compareTo(BigInteger.ZERO) > 0) {
                        logger.info("Positive balance found for Seed phrase: {}, Address: {}, Balance: {} WEI", seedPhraseString, credentials.getAddress(), wei);
                        saveSeedPhrase("C:\\trial_itellij", POSITIVE_BALANCE_FILE, seedPhraseString);

                    } else {
                        logger.info("Zero balance found for Seed phrase: {}", seedPhraseString);
                        saveSeedPhrase("C:\\trial_itellij", ZERO_BALANCE_FILE, seedPhraseString);
                    }
                    usedSeedPhrases.add(seedPhraseString);

                } catch (Exception e) {
                    logger.info("Error checking wallet balance for seed phrase: {}", String.join(" ", seedPhrase), e);
                }
            }, executor).exceptionally(ex -> {
                if (isExecutorActive(executor)) {
                if (ex.getCause() instanceof ClientConnectionException) {
                    logger.warn("ClientConnectionException occurred, switching to a different API and retrying");
                    if (API_URL_INDEX.incrementAndGet() >= API_URLS.size()) {
                        API_URL_INDEX.set(0);
                    }
                    Web3j newWeb3j = getWeb3jClient(httpClient);
                    return checkWalletBalanceAsync(newWeb3j, httpClient, seedPhrase, usedSeedPhrases, tasksCount, executor).join();
                } else {
                    logger.error("Unexpected exception occurred", ex);
                    return null;
                }
                } else {
                    logger.error("Executor is shutting down. Retry is not possible.", ex);
                    return null;
                }
            });
        }
    }

    private static List<String> loadSeedPhrases(String fileName) {
        List<String> seedPhrases = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                seedPhrases.add(line);
            }
            logger.info("Loaded {} seed phrases from file: {}", seedPhrases.size(), fileName);
        } catch (IOException e) {
            logger.info("Error reading file: {}", fileName, e);
        }
        return seedPhrases;
    }

    private static void saveSeedPhrase(String path, String fileName, String seedPhrase) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + File.separator + fileName, true))) {
            bw.write(seedPhrase);
            bw.newLine();
            logger.info("Seed phrase saved to file: {}", fileName);
        } catch (IOException e) {
            logger.info("Error writing to file: {}", fileName, e);
        }
    }


    private static boolean isValidSeedPhrase(List<String> seedPhrase) {
        try {
            MnemonicCode.INSTANCE.check(seedPhrase);
            logger.info("Seed phrase is valid: {}", String.join(" ", seedPhrase));
            return true;
        } catch (MnemonicException e) {
            return false;
        }
    }

    private static boolean isExecutorActive(ExecutorService executor) {
        return !executor.isShutdown() && !executor.isTerminated();
    }
}