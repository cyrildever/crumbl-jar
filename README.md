# crumbl-jar

![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/cyrildever/crumbl-jar)
![GitHub last commit](https://img.shields.io/github/last-commit/cyrildever/crumbl-jar)
![GitHub issues](https://img.shields.io/github/issues/cyrildever/crumbl-jar)
![NPM](https://img.shields.io/npm/l/crumbl-js)

crumbl-jar is both an executable to work in the JVM and a Scala client for generating secure data storage with trusted signing third-parties using the Crumbl&trade; technology patented by Cyril Dever for [Edgewhere](https://www.edgewhere.fr).

If you're interested in using the library and/or the whole Crumbl&trade; process, please [contact us](mailto:contact@edgewhere.fr).

### Formal description

For details about the mathematical and protocol foundations, please review the latest version of the [white paper](https://github.com/cyrildever/crumbl-exe/blob/master/documentation/src/latex/crumbl_whitepaper.pdf).


### Process

The whole process could be divided into two major steps:
* create the _crumbl_ from a source data;
* extract the data out of a _crumbl_.

The first step involves at least two stakeholders, but preferably four for optimal security and sustainability:
* at least one "owner" of the data, ie. the stakeholder that needs to securely store it;
* three signing trusted third-parties who shall remain unaware of the data.

1. Creation

    To create the _crumbl_, one would need the data and the public keys of all the stakeholders, as well as the encryption algorithm used by them.
    Currently, two encryption algorithms are allowed by the system: ECIES and RSA.

    Once created, the _crumbl_ could be stored by anyone: any stakeholder or any outsourced data storage system. 
    The technology guarantees that the _crumbl_ can't be deciphered without the presence of the signing stakeholders, the number of needed stakeholders depending on how many originally signed it, but a data owner must at least be present. In fact, only a data owner will be able to fully recover the original data from the _crumbl_.

2. Extraction

    To extract the data from a _crumbl_ is a multi-step process:
    * First, the data owner should ask the signing trusted third-parties to decipher the parts (the "crumbs") they signed;
    * Each signing trusted third-party should use their own key pair (private and public keys) along with the _crumbl_, and then return the result (the "partial uncrumbs") to the data owner;
    * After, collecting all the partial uncrumbs, the data owner should inject them in the system along with the _crumbl_ and his own keypair to get the fully-deciphered data.


All these steps could be done using command-line instructions with the [executable](#executable), or building an integrated app utilizing the [Scala library](#scala-library).


### Usage

#### Executable

```console
Crumbl 6.1.1
Usage: java -cp crumbl-jar-6.1.1.jar:bcprov-jdk15to18-1.69.jar:feistel-jar-1.4.0.jar io.crumbl.Main [options] [<data> ...]

  -c, --create             create a crumbled string from source
  -x, --extract            extract crumbl(s)
  -in, --input <value>     file to read an existing crumbl from (WARNING: do not add the crumbl string in the command-line arguments too)
  -out, --output <value>   file to save result to
  --owner-keys <value>     comma-separated list of colon-separated encryption algorithm prefix and filepath to public key of owner(s)
  --owner-secret <value>   filepath to the private key of the owner
  --signer-keys <value>    comma-separated list of colon-separated encryption algorithm prefix and filepath to public key of trusted signer(s)
  --signer-secret <value>  filepath to the private key of the trusted signer
  -vh, --verification-hash <value>
                           optional verification hash of the data
  -h, --help               prints this usage text
  <data> ...               data to use
```

_IMPORTANT: for security purpose, you must download the latest BouncyCastle JAR (eg. `bcprov-jdk15to18-1.64.jar`) and refer to it in the classpath of your command-line along with the Crumbl&trade; JAR as well as our latest Feistel lib JAR (`feistel-jar-1.3.4.jar`)._

1. Creation

    To create a _crumbl_, you need to pass the `-c` flag, then to fill in the `--owner-keys` and `--signer-keys` flags in the appropriate format concatenating:
    * the code name of the encryption algorithm to use (`ecies` or `rsa`);
    * a separating colon (`:`);
    * the path to the file holding the public key (using the uncompressed public key in ECIES, and a PEM file for RSA).
    eg. `ecies:path/to/myKey.pub`

    Optionally, you may add the file path to the `-out` flag to save the result into.

    The data to crumbl should be placed at the end of the command line.

    For example, here is a call to crumbl the data `myDataToCrumbl`:
    ```console
    $ java -cp target/scala-2.12/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.69.jar:feistel-jar-1.4.0.jar io.crumbl.Main -c -out myFile.dat --owner-keys ecies:path/to/myKey.pub --signer-keys ecies:path/to/trustee1.pub,rsa:path/to/trustee2.pub myDataToCrumbl
    SUCCESS - crumbl successfully saved to myFile.dat
    ```

    Not filling the `-out` flag results in sending the _crumbl_ to stdout.

2. Extraction

    i. Get the partial uncrumbs from the signing trusted third-parties

    When asked (generally by sending the _crumbl_ over to them), each signing trusted third-party should use the executable to get the partial uncrumbs, ie. the deciphered crumbs the system assigned them to sign upon creation.

    The signer should pass the `-x` flag, then fill the `--signer-keys` flag with the algorithm and public key information as above and the `--signer-secret` with the path to the file holding the corresponding private key.

    Optionally, the signer may add the file path to the `-out` flag to save the result into.

    The _crumbl_ should be placed either at the end of the command line, or in a file to reference in the `-in` flag.

    For example, here is a call to partially uncrumbl a _crumbl_ placed in a file:
    ```console
    $ java -cp target/scala-2.12/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.69.jar:feistel-jar-1.4.0.jar io.crumbl.Main -x -in theCrumbl.dat --signer-keys rsa:path/to/trustee2.pub --signer-secret path/to/trustee2.sk
    123fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%01AgICAgKWqJ/v0/4=.1
    ```
    The second line above is an example of partial uncrumb sent to stdout because the `-out` wasn't defined.

    ii. Fully-decipher the _crumbl_ as the owner

    After receiving every partial uncrumbs from the signing trusted third-parties, the data owner can fully uncrumbl the _crumbl_.

    The owner should pass the `-x` flag, then fill the `--owner-keys` flag with the algorithm and public key information as above and the `--owner-secret` with the path to the file holding his corresponding private key.

    Optionally, the owner may add the file path to the `-out` flag to save the result into.
    He should also provide the `-vh` tag with the stringified value of the hash of the original data. As of the latest version, this hash should use the SHA-256 hash algorithm.

    The partial uncrumbs could have been appended using a separating space to the end of the file used in the `-in` flag, or to the string of the _crumbl_ passed at the end of the command line. Alternatively, the _crumbl_ could be passed using the `-in` flag and the partial uncrumbs passed at the end of the command line.

    For example, here is a call to get the _crumbl_ deciphered using the last scenario:
    ```console
    $ java -cp target/scala-2.12/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.69.jar:feistel-jar-1.4.0.jar io.crumbl.Main -x -in theCrumbl.dat -vh 123fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d --owner-keys ecies:path/to/myKey.pub --owner-secret path/to/myKey.sk 123fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%01AgICAgKWqJ/v0/4=.1 123fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%02AgICAgKEEqTinyo=.1
    myDataToCrumbl
    ```

As of the latest version, the technology only processes one crumbl at a time.

NB: Error(s) and/or warning message(s) are all sent to stderr.

#### Scala Library

```
libraryDependencies += "fr.edgewhere" %% "crumbl-jar" % "6.1.1"
```

Construct a new CrumblWorker client by passing to it all the arguments otherwise passed in the executable as flags (see above).
Then, launch its process.

For example, the code below reproduces the command-line instruction above for crumbl creation:
```scala
import io.crumbl.client._
import CrumblWorker._

val worker = CrumblWorker(
      mode =             CREATION,
      input =            None,
      output =           Some("myFile.dat"),
      ownerKeys =        Some("ecies:path/to/myKey.pub"),
      ownerSecret =      None,
      signerKeys =       Some("ecies:path/to/trustee1.pub,rsa:path/to/trustee2.pub"),
      signerSecret =     None,
      verificationHash = None,
      data =             Seq("myDataToCrumbl")
)
val result = worker.process()
// Do sth with the result
```
_NB: You may pass `false` to the `process()` method not to return any result, ie. mimic the behaviour of the executable._


#### Javascript Library

You might want to check out the JS implementation for the Crumbl&trade;: [`crumbl-js`](https://github.com/cyrildever/crumbl-js), a Javascript client developed in TypeScript for generating secure data storage with trusted signing third-parties using the Crumbl&trade; technology patented by Cyril Dever for Edgewhere.


#### Go Library

You might also want to check out the Go implementation for the Crumbl&trade;: [`crumbl-exe`](https://github.com/cyrildever/crumbl-exe), a Go client and an executable as well.


### License

The use of the Crumbl&trade; executable or library is subject to fees for commercial purpose and to the respect of the [BSD-2-Clause-Patent](LICENSE) terms for everyone.
All technologies are protected by patents owned by Edgewhere.
Please [contact us](mailto:contact@edgewhere.fr) to get further information.


<hr />
&copy; 2019-2023 Cyril Dever. All rights reserved.