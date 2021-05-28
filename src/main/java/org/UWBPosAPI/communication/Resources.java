/**
* @author  Thomas Jennings
* @since   2020-03-25
*/

package org.UWBPosAPI.communication;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.UWBPosAPI.model.Tag;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

@javax.ws.rs.Path("Tag")

@ApplicationScoped
public class Resources {

	// set this for the location of the wallet directory and the connection json
	// file

	Path currentUsersHomeDir = Paths.get(System.getProperty("user.dir"));
	Path pathRoot = Paths.get(currentUsersHomeDir.toString(), "Users", "Shared", "Connections");
	String connectionFile = "\\UwbTestOrg1GatewayConnection.json";
	static {
		System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
	}

	// #region TRANS:AddTag
	@Timed(name = "addTAGProcessingTime", tags = {
			"method=post" }, absolute = true, description = "Time needed to add TAG to the inventory")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@javax.ws.rs.Path("createTag")
	@Operation(summary = "Add a new Tag to the ledger", description = "Requires a unique key starting with TAG to be successfull")
	public Tag addTag(Tag aTag) {
		try {
			Path walletPath = Paths.get(pathRoot.toString(), "wallet");
			Wallet wallet = Wallets.newFileSystemWallet(walletPath);

			// load a CCP
			// expecting the connect profile json file; export the Connection Profile from
			// the
			// fabric gateway and add to the default server location
			Path networkConfigPath = Paths.get(pathRoot + connectionFile);
			Gateway.Builder builder = Gateway.createBuilder();

			// expecting wallet directory within the default server location
			// wallet exported from Fabric wallets Org 1
			builder.identity(wallet, "sachi").networkConfig(networkConfigPath).discovery(true);
			// expecting wallet directory within the default server location
			// wallet exported from Fabric wallets Org 1
			try (Gateway gateway = builder.connect()) {
				// get the network and contract
				Network network = gateway.getNetwork("mychannel");
				Contract contract = network.getContract("tag");
				contract.submitTransaction("createTag", aTag.getTagID(), aTag.getTimestamp(), aTag.getTagName(), aTag.getCategory());
				return new Tag(aTag.getTagID(), aTag.getTimestamp(), aTag.getTagName(), aTag.getCategory());
			} catch (Exception e) {
				System.out.println("Unable to get network/contract and execute query");
				throw new javax.ws.rs.ServiceUnavailableException();
			}
		} catch (Exception e2) {
			String current;
			try {
				current = new java.io.File(".").getCanonicalPath();
				System.out.println("Current working dir: " + current);
			} catch (IOException e) {
				throw new javax.ws.rs.ServiceUnavailableException();
			}
			System.out
					.println("Unable to find config or wallet - please check the wallet directory and connection json");
			throw new javax.ws.rs.ServiceUnavailableException();
		}
	}

	@Timed(name = "addTAGProcessingTime", tags = {
			"method=post" }, absolute = true, description = "Time needed to add TAG to the inventory")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@javax.ws.rs.Path("createTagTest")
	@Operation(summary = "Add a new Tag to the ledger", description = "Requires a unique key starting with TAG to be successfull")
	public Tag addTagTEST(@QueryParam("TagID") String key, @QueryParam("Timestamp") String time,
			@QueryParam("Name") String name, @QueryParam("Category") String cat) {
		try {

			Path networkConfigPath = Paths.get(pathRoot + connectionFile);

			NetworkConfig ccp = NetworkConfig.fromJsonFile(networkConfigPath.toFile());
			// initialize default cryptosuite and setup the client
			CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
			HFClient client = HFClient.createNewInstance();

			client.setCryptoSuite(cryptoSuite);

			Channel channel = client.loadChannelFromConfig(ccp.getChannelNames().iterator().next(), ccp);
			channel.initialize();

			TransactionProposalRequest transactionProposal = client.newTransactionProposalRequest();
			// build chaincode id providing the chaincode name
			ChaincodeID mychaincodeID = ChaincodeID.newBuilder().setName("tag").build();
			transactionProposal.setChaincodeID(mychaincodeID);
			// calling chaincode function
			transactionProposal.setFcn("createTag");
			transactionProposal.setArgs(key, time, name, cat);

			Collection<ProposalResponse> res = channel.sendTransactionProposal(transactionProposal);
			channel.sendTransaction(res);
			return new Tag(key, time, name, cat);
		} catch (Exception e2) {
			String current;
			try {
				current = new java.io.File(".").getCanonicalPath();
				System.out.println("Current working dir: " + current);
				System.out.println("Current Tag: " + new Tag(key, time, name, cat).toJSONString());
			} catch (IOException e) {
				System.out.println("Error Infor:" + e.toString());
				throw new javax.ws.rs.ServiceUnavailableException();
			}
			System.out
					.println("Unable to find config or wallet - please check the wallet directory and connection json");
			throw new javax.ws.rs.ServiceUnavailableException();
		}
	}
	// #endregion

	// #region Query:AllTags
	@Timed(name = "QueryTAGSProcessingTime", tags = {
			"method=GET" }, absolute = true, description = "Time needed to query all Tags")
	@GET
	@javax.ws.rs.Path("getAllTags")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Returns all Tags Registered in the network", description = "No input required")
	public String queryTags() {

		byte[] result = null;
		String outputString = "";
		String passedOutput = "";

		try {
			Path walletPath = Paths.get(pathRoot.toString(), "wallet");
			Wallet wallet = Wallets.newFileSystemWallet(walletPath);

			// load a CCP
			// expecting the connect profile json file; export the Connection Profile from
			// the
			// fabric gateway and add to the default server location
			Path networkConfigPath = Paths.get(pathRoot + connectionFile);
			Gateway.Builder builder = Gateway.createBuilder();

			// expecting wallet directory within the default server location
			// wallet exported from Fabric wallets Org 1
			builder.identity(wallet, "sachi").networkConfig(networkConfigPath).discovery(true);

			try (Gateway gateway = builder.connect()) {

				// get the network and contract
				Network network = gateway.getNetwork("mychannel");
				Contract contract = network.getContract("tag");
				result = contract.evaluateTransaction("getAllTags");
				outputString = new String(result);
				passedOutput = "Queried all Tags Successfully. \nTags are:\n " + outputString;
				return passedOutput;
			} catch (Exception e) {
				System.out.println("Unable to get network/contract and execute query");
				throw new javax.ws.rs.ServiceUnavailableException();
			}
		} catch (Exception e2) {
			String current;
			try {
				current = new java.io.File(".").getCanonicalPath();
				System.out.println("Current working dir: " + current);
				System.out.println("Current Path to config: " + pathRoot);
				System.out.println("Current config: " + Paths.get(pathRoot + connectionFile));
			} catch (IOException e) {
				throw new javax.ws.rs.ServiceUnavailableException();
			}
			System.out
					.println("Unable to find config or wallet - please check the wallet directory and connection json");
			throw new javax.ws.rs.ServiceUnavailableException();
		}
	}
	// #endregion

	@Timed(name = "UpdateTagName", tags = {
			"method=put" }, absolute = true, description = "Time needed to update car in the inventory")

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@javax.ws.rs.Path("UpdateTagName")

	@Operation(summary = "Update Name of a Tag in the ledger", description = "Requires the TagID to the Tag and a new Tag name")
	public Tag UpdateTagName(Tag tag) {
		try {
			Path walletPath = Paths.get(pathRoot.toString(), "wallet");
			Wallet wallet = Wallets.newFileSystemWallet(walletPath);

			// load a CCP
			// expecting the connect profile json file; export the Connection Profile from
			// the
			// fabric gateway and add to the default server location
			Path networkConfigPath = Paths.get(pathRoot + connectionFile);
			Gateway.Builder builder = Gateway.createBuilder();

			// expecting wallet directory within the default server location
			// wallet exported from Fabric wallets Org 1
			builder.identity(wallet, "sachi").networkConfig(networkConfigPath).discovery(true);
			try (Gateway gateway = builder.connect()) {

				// get the network and contract
				Network network = gateway.getNetwork("mychannel");
				Contract contract = network.getContract("tag");
				contract.submitTransaction("updateTagName", tag.getTagID(), tag.getTimestamp(), tag.getTagName());
				return new Tag(tag.getTagID(), tag.getTimestamp(), tag.getTagName(), tag.getCategory());
			} catch (Exception e) {
				System.out.println("Unable to get network/contract and execute query");
				throw new javax.ws.rs.ServiceUnavailableException();
			}
		} catch (Exception e2) {
			String current;
			try {
				current = new java.io.File(".").getCanonicalPath();
				System.out.println("Current working dir: " + current);
			} catch (IOException e) {
				throw new javax.ws.rs.ServiceUnavailableException();
			}
			System.out
					.println("Unable to find config or wallet - please check the wallet directory and connection json");
			throw new javax.ws.rs.ServiceUnavailableException();
		}
	}

	// #region Query:TagHistory
	@Timed(name = "QueryTagHistory", tags = {
			"method=GET" }, absolute = true, description = "Time needed to query a Tag History")
	@GET
	@javax.ws.rs.Path("getTagHistory")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Returns Tag History of an individual Tag by TagID", description = "Requires the key to be provided")
	public String QueryTagHistory(@QueryParam("TagID") String Key) {

		byte[] result = null;
		String outputString = "";
		String passedOutput = "";

		try {
			Path walletPath = Paths.get(pathRoot.toString(), "wallet");
			Wallet wallet = Wallets.newFileSystemWallet(walletPath);
			Path networkConfigPath = Paths.get(pathRoot + connectionFile);
			Gateway.Builder builder = Gateway.createBuilder();

			builder.identity(wallet, "sachi").networkConfig(networkConfigPath).discovery(true);
			try (Gateway gateway = builder.connect()) {
				Network network = gateway.getNetwork("mychannel");
				Contract contract = network.getContract("tag");
				result = contract.evaluateTransaction("queryTagHistory", Key);
				outputString = new String(result);
				passedOutput = "Queried Tag Successfully. \nKey = " + Key + "\nDetails = " + outputString;
				return passedOutput;
			} catch (Exception e) {
				System.out.println("Unable to get network/contract and execute query");
				System.out.println(e.toString());
				throw new javax.ws.rs.ServiceUnavailableException();
			}
		} catch (Exception e2) {
			String current;
			try {
				current = new java.io.File(".").getCanonicalPath();
				System.out.println("Current working dir: " + current);
			} catch (IOException e) {
				throw new javax.ws.rs.ServiceUnavailableException();
			}
			System.out
					.println("Unable to find config or wallet - please check the wallet directory and connection json");
			throw new javax.ws.rs.ServiceUnavailableException();
		}
	}
	// #endregion

}
