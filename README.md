# Jobcoin Mixer

## REST API

**GET**  */status* returns status of the Jobcoin app

**GET**  */v1/commands* returns all available API endpoints available to client, **call this for a description of each endpoint and required input**

**GET**  */v1/balance* retrieves balance of an address

**GET**  */v1/mixingStatus* gets status of provided requestId

**POST** */v1/register* registers provided address list and returns deposit address

**POST** */v1/send* sends Jobcoins from and to provided addresses, registers Jobcoins to be mixed and returns associated requestId

## Running an instance

The simplest way to run this application is by cloning the repo into IntelliJ, importing as a gradle project and running the [MixerEntry.java](https://github.com/earce/jobcoin-mixer/blob/main/src/main/java/com/gemini/jobcoin/MixerEntry.java) class. In the spirit of simplicity all app configuration is done in code.

A production grade version of this application would take in a config file which would allow to configure things like threads and Verticles without recompiling.

*Alternatively* for commandline users (linux-only):

```bash script
$ git clone https://github.com/earce/jobcoin-mixer.git
$ cd jobcoin-mixer
$ chmod +x gradlew
$ ./gradlew build
$ java -cp build/libs/jobcoin-mixer-1.0-SNAPSHOT.jar com.gemini.jobcoin.MixerEntry
```

This should startup the application on localhost:8111

**Note:** Logging is very limited in this application because most of the errors are captured and returned to the client. Logging is a bit subjective and opinionated, so I have opted to keep the logging minimal.

## Using the mixer

Using the mixer involves several steps:

1. First using */v1/register*, register the destination addresses where you want the final amount of money to get sent to. This API will return a deposit address used to make funds available to the Jobcoin Mixer.
2. Next, using */v1/send* send money from your source address to the deposit address provided by the previous command. Under the hood this will transfer funds from the deposit address to the mixers house address. After that it will register your request with the mixer. This API will return a unique requestId to track the status of your mixing request. 
3. Finally, using */v1/mixingStatus* you can check if the mixing is still in process.

At any point */v1/balance* can be used to check the balance of any address.

## Curl examples for aformentioned usage

**/v1/register**

```bash
curl --header "Content-Type: application/json" -X POST --data '["John","Alice", "Bob"]' localhost:8111/v1/register
```

**/v1/balance**

```bash
curl -X GET localhost:8111/v1/balance?address=Erick
```

## Privacy optimizations (implemented)

Provided amount is broken up into n smaller decimal amounts which are a random selected size and sum the original amount. See [JobcoinMath.java](https://github.com/earce/jobcoin-mixer/blob/main/src/main/java/com/gemini/jobcoin/helper/JobcoinMath.java)

These different quantities are sent in random intervals with a 20s upper bound (configurable) to a randomly chosen address from the list of provided addresses in the register command. See [MixingEngine.java](https://github.com/earce/jobcoin-mixer/blob/main/src/main/java/com/gemini/jobcoin/verticles/MixingEngine.java)

## Privacy optimizations (not implemented)


For a mixer to work well, other users would need to be involved in the mixing process at the same time, the more users the better the protection this service offers. This was considered but aside from waiting for a balance to accumulate on the house address, or waiting for at least n number of user to be involved in a time window there isn't a particularly elegant way to accomplish this.

To anonymize this even further, Jobcoin could charge a randomized fee within a range 0.05 - 0.1 % which would make tieing out the starting quantity with the ending quantity even harder. Users would expect to accept a variable fee.

Another step to anonymize this and make tracking difficult would be single use or time limited deposit addresses. The deposit address provided by Jobcoin would have a limited use there by forcing use of fresh addresses.

## Testing

Code Coverage (Instructions) <kbd>~75%</kbd> 

How to view code coverage as webpage

```bash script
$ ./gradlew build
$ ./gradlew coverageReport
$ cd build/reports/jacoco/test/html
```

Then you should be able to click on index.html to view it as a webpage.

## Notes and other things considered

Vertx was chosen because it is a well known reactor pattern (event driven) framework that makes bootstrapping a lot of the HTTP server portions fairly fast.

In a fully realized version of this application persistence and cross instance guarantees would be a lot bigger part of this. Several classes found in the **com.gemini.jobcoin.persistence** package are mean to simulate this.

The requirements for this application to be completely fault tolerate are significant, especially if one aims to reuse destination addresses as well as deposit addresses and handle modifying their values concurrently. A strong transaction system would need to exist in the Gemini API with individual transaction ids that could be used to tie out what has been send where by who in the event of system a failure. 

The balance/transaction APIs do not tell us enough information about who balances belong to.