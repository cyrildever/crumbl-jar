# Example usage

The following describes a working example of how to use the Crumbl&trade; executable in the JVM:

1. Create a crumbl with all stakeholders

    ```
    java -cp target/scala-2.13/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.64.jar io.crumbl.Main -c --owner-keys ecies:src/test/resources/keys/owner1.pub --signer-keys ecies:src/test/resources/keys/trustee1.pub,rsa:src/test/resources/keys/trustee2.pub cdever@edgewhere.fr
    ```
    You may add the `-out path/to/exampleCrumbl.dat` flag to save the result to a `exampleCrumbl.dat` file.

2. Ask the trustees to decipher their crumbs

    For the first trustee, using ECIES encryption:
    ```
    java -cp target/scala-2.13/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.64.jar io.crumbl.Main -x --signer-keys ecies:src/test/resources/keys/trustee1.pub --signer-secret src/test/resources/keys/trustee1.sk 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d0000a8BIHuhYKKUwPqFY+e1TpnJUhCva9EghtLvl9W7B1i5HKW7s7eXOMlfmKqJUHIj4MuuYvJmI8ptva6ijVjAHH7QpvhIM8uWCNHqj04Tr/mtK78jz3E8OIflc4FXpsbQj12C6GH8p7Lv6d6l14pPecSLVgcQ/ovsPFnC3fKJw==010158D2M61pYiQBspzGLzsUpAmjIIsjHtmMH/5orCf/due6JSUSzOJCqNCm/rs+wbd4VHXijTRxbFADTUDoUy3nv4IJyUnFsXwUFB4a3mvx9HO9i47+0IgRkpDoxRvDBdHxVVlvBVR37ld3eZDKeAn69IdGkhkxOb9HBb4xzux3u1dgBvbtpw+6toNV5z3LWgM+c0NRNtRjKNMAR5QBH8bYOCQjAfWeTlZS83Q5cB6KeaAQOmRx+8+5oU6jzX6tcw98K2ivl8r6BU+rhSLaxzD2SCAqhY+LDXtObtTUQNVulJMqU6cpWh0AwqZ21yqkMvB8PqVqxAmi/a9Yy5mwHtRj3jHg==0200a8BFp7WtWEW3zJzXCvwRXneYhqYGIBNhm4tyBVmPDI+WN/DNGj4pYRAuvljIBCZZU4IG+EHIvi82FEjR991h6+ZNYBP0WpLRwvITwfzb0gbz3fgDKnCVUjasZjlapQrQ51Um8IYImFggUGOFPm1sZys0QfSBcHisKDpP5nKA==.1
    ```
    The result should be: `580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%02AgICAgICAgkYUkI=.1`

    For the second trustee, using RSA encryption and an input file:
    ```
    java -cp target/scala-2.13/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.64.jar io.crumbl.Main -x -in exampleCrumbl.dat --signer-keys rsa:src/test/resources/keys/trustee2.pub --signer-secret src/test/resources/keys/trustee2.sk
    ```
    The result should be: `580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%01AgICBFUDDk4PXEc=.1`

    Here again, you may add the `-out` flag to save these results to files.

3. Finalize the extraction as the owner

    ```
    java -cp target/scala-2.13/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.64.jar io.crumbl.Main -x --owner-keys ecies:src/test/resources/keys/owner1.pub --owner-secret src/test/resources/keys/owner1.sk -vh 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d0000a8BIHuhYKKUwPqFY+e1TpnJUhCva9EghtLvl9W7B1i5HKW7s7eXOMlfmKqJUHIj4MuuYvJmI8ptva6ijVjAHH7QpvhIM8uWCNHqj04Tr/mtK78jz3E8OIflc4FXpsbQj12C6GH8p7Lv6d6l14pPecSLVgcQ/ovsPFnC3fKJw==010158D2M61pYiQBspzGLzsUpAmjIIsjHtmMH/5orCf/due6JSUSzOJCqNCm/rs+wbd4VHXijTRxbFADTUDoUy3nv4IJyUnFsXwUFB4a3mvx9HO9i47+0IgRkpDoxRvDBdHxVVlvBVR37ld3eZDKeAn69IdGkhkxOb9HBb4xzux3u1dgBvbtpw+6toNV5z3LWgM+c0NRNtRjKNMAR5QBH8bYOCQjAfWeTlZS83Q5cB6KeaAQOmRx+8+5oU6jzX6tcw98K2ivl8r6BU+rhSLaxzD2SCAqhY+LDXtObtTUQNVulJMqU6cpWh0AwqZ21yqkMvB8PqVqxAmi/a9Yy5mwHtRj3jHg==0200a8BFp7WtWEW3zJzXCvwRXneYhqYGIBNhm4tyBVmPDI+WN/DNGj4pYRAuvljIBCZZU4IG+EHIvi82FEjR991h6+ZNYBP0WpLRwvITwfzb0gbz3fgDKnCVUjasZjlapQrQ51Um8IYImFggUGOFPm1sZys0QfSBcHisKDpP5nKA==.1 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%01AgICBFUDDk4PXEc=.1 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%02AgICAgICAgkYUkI=.1
    ```
    The uncrumbled data (sent to stdout) should be: `cdever@edgewhere.fr`

    Alternatively, you may use an input file for the crumbl:
     ```
    java -cp target/scala-2.13/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.64.jar io.crumbl.Main -x -in exampleCrumbl.dat --owner-keys ecies:src/test/resources/keys/owner1.pub --owner-secret src/test/resources/keys/owner1.sk -vh 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%01AgICBFUDDk4PXEc=.1 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%02AgICAgICAgkYUkI=.1
    ```

As of the latest version, the library only processes one crumbl at a time, ie. only the first line in an input file.