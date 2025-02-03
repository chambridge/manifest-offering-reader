# manifest-offering-reader
A simple java project that reads a manifest export and collects the subscription capacity information found.


## Getting Started

Build using Maven:
```
mvn clean package
```

## Run

Download a manifest zip file from console.redhat.com

```
java -jar target/manifest-reader-1.0-SNAPSHOT.jar <path-to-manifest-zip>
```

What output looks like:

```
Processing JSON file: export/entitlements/2503367ffbbf4b6a919d15f53239eefe.json
Processing JSON file: export/entitlements/88af02657de3493ea4642fdd65d759ea.json
Processing JSON file: export/entitlements/b1bc33c4f1604ffdb512a310ec913d06.json
Processing JSON file: export/entitlements/da87859e8ce344f4a82a6a5aab341159.json
Processing JSON file: export/entitlements/f703baa07703467fb80e6a75a52d53b1.json
{product_family=Red Hat Enterprise Linux, service_type=Self-Support, quantity=16, productId=RH00798, usage=Development/Test, description=Red Hat Enterprise Linux, sockets=2048, productName=Red Hat Developer Subscription for Individuals}
{product_family=Red Hat Enterprise Linux, service_type=Premium, quantity=5536, productId=RH00003, L1=true, L2=true, usage=Production, description=Red Hat Enterprise Linux, sockets=11072, productName=Red Hat Enterprise Linux Server, Premium (Physical or Virtual Nodes)}
{product_family=Employee SKU, service_type=Self-Support, quantity=125000, productId=ES0113909, L1=true, L2=true, usage=Development/Test, description=Employee SKU, sockets=16000000, productName=Employee SKU}
{product_family=Red Hat Enterprise Linux, service_type=Standard, quantity=200, productId=RH00009F3, L1=true, L2=true, usage=Production, description=Red Hat Enterprise Linux, sockets=400, productName=Red Hat Enterprise Linux Server with Satellite, Standard (Physical or Virtual Nodes)}
{product_family=Employee SKU, service_type=Self-Support, quantity=50, productId=ES0113909, L1=true, L2=true, usage=Development/Test, description=Employee SKU, sockets=6400, productName=Employee SKU}
```
