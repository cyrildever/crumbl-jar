# Example usage

The following describes a working example of how to use the Crumbl&trade; executable in the JVM:

1. Create a crumbl with all stakeholders

    ```
    java -cp target/scala-2.12/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.76.jar io.crumbl.Main -c --owner-keys ecies:src/test/resources/keys/owner1.pub --signer-keys ecies:src/test/resources/keys/trustee1.pub,rsa:src/test/resources/keys/trustee2.pub cdever@edgewhere.fr
    ```
    You may add the `-out path/to/exampleCrumbl.dat` flag to save the result to a `exampleCrumbl.dat` file.

2. Ask the trustees to decipher their crumbs

    For the first trustee, using ECIES encryption:
    ```
    java -cp target/scala-2.12/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.76.jar io.crumbl.Main -x --signer-keys ecies:src/test/resources/keys/trustee1.pub --signer-secret src/test/resources/keys/trustee1.sk 580fb8a91f05833200dea7d33536aaec990d596924455990c20378d960eeb1be0000a8BHGyTE7dAX6q4Eg4RlTxI50D6/wdR84mD1GS3HJUWPz8zBprQr+iltEW1nexSpLz+0dSgvDlf5FRc7CADkyPD5YtOHyNZt3nu1mwEyqzU9NVWpki848wLfO945WeLYQNsG5YrlnK9rNK3tHmvnh/gFUl7kWAKNJe6NoFuw==0100a8BPemmd/YgSelAr9vHfosGk29VtZ/DuwDmCUkWhDwgF8eOZSQluHLlV8YSE3c6mhOZT7MWJ6uXNQ5dVmZdgPAt+jh00Iu7aIPskDrEBm0LX96dGNDuepfbRno2mNdk+8NPaMkDUtdmSEPHVWZMFhYWh6yel8qXofMKufHdQ==020158JXT8koyYnDaVted+sgSKmdDCpkcXAFgu1SxCtdC/e55LLx9vVxBbq53z9lhlhIpapuFMpxnaQis/boEhQlG7pL/FyHWLudS3tGdyhUSzs5eWeh/F/XXaf1MZ0aahRlWbMa7uz3oas+ATJf/z9JI3QBSxnVTIEVnSpE6vp7u3BiElR5JKl7FX6BN8zftGlsl+WN7dMPcSJ0H72+o9DH/IizW7GHmeM+p2CG1nqPbd1Kg6NJqtlju6+xS43lWAciDVi1CPfWtou5SO7hlp+JQqlXR00ZE3Gegx7eYPj00BMCoJfN3mKQ/gDYLyJn4whntITAhz2aV3AY5/Zs2F8oGAdA==.1
    ```
    The result should be: `580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%01AgIEVQMOTg9cRwk=.1`

    For the second trustee, using RSA encryption and an input file:
    ```
    java -cp target/scala-2.12/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.76.jar io.crumbl.Main -x -in exampleCrumbl.dat --signer-keys rsa:src/test/resources/keys/trustee2.pub --signer-secret src/test/resources/keys/trustee2.sk
    ```
    The result should be: `580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%02AgICAgICAgIYUkI=.1`

    Here again, you may add the `-out` flag to save these results to files.

3. Finalize the extraction as the owner

    ```
    java -cp target/scala-2.12/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.76.jar io.crumbl.Main -x --owner-keys ecies:src/test/resources/keys/owner1.pub --owner-secret src/test/resources/keys/owner1.sk -vh 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d 580fb8a91f05833200dea7d33536aaec99df7af5098d296c1bf90c28cebf76d70000a8BKOR0GMVcYJzGjzJ6AU2Sq15QrxbTql/gkWpW5mddRjqpnmDym4HdkBl9Pt9bSPYEcXiBqJqUcTm0oZIevcqWlZPjM4GbBdL6qgR/wBAXZ1I1zah4czAJBMKMdbmOyuAo06NsCu0bqX0DkcZt2UuOax9ATVQymOiDwHzSg==0100a8BHYwWaREFtdRInDh5c/mweXM0SitlvO7hDzlAC8F3TvpYewDaBn4/FiFonKbINTwPQeoibIkXMFw+HKkXn5FEJ8pNtWalbgAFoO2Iy++FguMOt0aH00PVyV4LNBrEy9RUx5C1tJ5kxM6wTua/3mXsOMtXcjoVPIldF+zLg==020158ciYehulbTIg2p8EueDXRWl7EmnSsLo+sbOPsogMamWxs0MkzXhqPhpTfgKsBnnQ4g32rnwvQs5JyVfOR33LKz+XIgiwSSszOE96TTSp6EMDlEuijp/PK1qmCha/TAJ+o8odJy6tasON/OwWCRUq6hX+ZwKaw1s+xYWpsK7u1ugAtitjpo66xt0LZTtZpmAqT9jZa+1CXQLUsT/TGHovsq45GZJe9zLnuPgxVKjbHrS9xHKSU1P7aQ7zcaZwh+PAmxu+egFXC5U5YYeFmoCBXbN9dk8/sG+qb/T5Z1wVhcogVic6aYNmigxY/DpoErUCooyEGwJVunu+7vpgFeMnz+w==.1 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%01AgIEVQMOTg9cRwk=.1 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%02AgICAgICAgIYUkI=.1
    ```
    The uncrumbled data (sent to stdout) should be: `cdever@edgewhere.fr`

    Alternatively, you may use an input file for the crumbl:
     ```
    java -cp target/scala-2.12/crumbl-jar-<version>.jar:bcprov-jdk15to18-1.76.jar io.crumbl.Main -x -in exampleCrumbl.dat --owner-keys ecies:src/test/resources/keys/owner1.pub --owner-secret src/test/resources/keys/owner1.sk -vh 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%01AgIEVQMOTg9cRwk=.1 580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%02AgICAgICAgIYUkI=.1
    ```

As of the latest version, the library only processes one crumbl at a time, ie. only the first line in an input file.