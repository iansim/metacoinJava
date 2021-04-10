package com.test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.tx.response.NoOpProcessor;
import org.web3j.utils.Numeric;

/**
 * Hello world!
 *
 */
public class App {
	public static String address = "0xB3B5c03054ECF1691A3E1889E90d2F4930b31e40";
	public static String contractAddress = "0xE900939B709321b27afD5Bbe1E9b4213085463cF";
	public static String privateKey = "0x9905450c0112f9bddc12778aab1b060c7491d94c0bb4c669c46d1896845bde90";
	public static String orgaddress = "0x90Af934A1675e8dB26389406B5f5a651eB47FB18";

	public static BigInteger getBalance(Web3j web3j, String fromAddress) {
		BigInteger balance = null;
		try {
			EthGetBalance ethGetBalance = web3j.ethGetBalance(fromAddress, DefaultBlockParameterName.PENDING).send();
			balance = ethGetBalance.getBalance();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("address " + fromAddress + " balance " + balance + " wei");
		return balance;
	}
	
	public static RawTransaction createRawtransaction(Web3j web3j,Credentials credentials, Function function,String contracAddress) throws IOException {
		EthGetTransactionCount ethGetTransactionCount = 
				web3j.ethGetTransactionCount(credentials.getAddress(),DefaultBlockParameterName.LATEST).send();
		BigInteger nonce = ethGetTransactionCount.getTransactionCount();
		StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(0),BigInteger.valueOf(20000000));
		String encodedData = FunctionEncoder.encode(function);
		RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, BigInteger.ZERO,gasProvider.getGasLimit(function.getName()),
				contracAddress,BigInteger.ZERO, encodedData);
		return rawTransaction;
	}
	
	public static void signAndSendRawTransaction(Web3j web3j,Credentials credentials,RawTransaction rawTransaction) throws IOException {
		byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
		String hexValue = Numeric.toHexString(signedMessage);
		EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
		String transactionHash = ethSendTransaction.getTransactionHash();
		System.out.println("transactionHash:" + transactionHash);
		Optional<TransactionReceipt> receipt = null;
		do {
			System.out.println("checking if transaction "+transactionHash+" is mined....");
			EthGetTransactionReceipt rsp = web3j.ethGetTransactionReceipt(transactionHash).send();
			receipt = rsp.getTransactionReceipt();
		}while(!receipt.isPresent());
	}

	public static Function getContractSenCoin(String receiver, BigInteger amount) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
        		MetaCoin.FUNC_SENDCOIN, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(receiver), 
                new org.web3j.abi.datatypes.generated.Uint256(amount)), 
                Collections.<TypeReference<?>>emptyList());
        return function;
	}
	

	public static void main(String[] args) throws Exception {
		System.out.println("Hello World!");
		Web3j web3j = Web3j.build(new HttpService("http://localhost:8545"));
		Credentials credentials = Credentials.create(privateKey);
		System.out.println("Address:" + credentials.getAddress());
		System.out.println("Balance:" + getBalance(web3j,credentials.getAddress()));
//		Function function = getContractSenCoin(orgaddress,BigInteger.valueOf(1));
//		RawTransaction rawtransaction = App.createRawtransaction(web3j, credentials, function, contractAddress);
//		App.signAndSendRawTransaction(web3j, credentials, rawtransaction);

		//NoOpProcessor processor = new NoOpProcessor(web3j);
		TransactionManager txManager = new FastRawTransactionManager(web3j,credentials);
		StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(0),BigInteger.valueOf(20000000));
		MetaCoin instance = MetaCoin.load(contractAddress, web3j, txManager, gasProvider);
		TransactionReceipt receipt = instance.sendCoin(orgaddress, BigInteger.valueOf(1)).send();
		System.out.println("receipt:"+receipt);
		BigInteger cb = instance.getBalance(orgaddress).send();
		System.out.println("Private Balance:"+cb);
		BigInteger cb1 = instance.getBalance(credentials.getAddress()).send();
		System.out.println("Private Balance:"+cb1);
	}
}
