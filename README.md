# inspector-lambda

A lambda to:

* Find all combinations of the App, Stack, Stage tags (including `not present`)
* Find the youngest `n` running instances of each combination
* Add a unique tag to each of the instances
* Schedule an AWS Inspector run against that tag

## Running locally

To run the lambda locally make sure you have Janus credentials for the corresponsing account and run 

```
sbt ';run <account name>'
```

For instance 

```
sbt ';run security'
```

for the **security** account.

## Deployment

### Local development and deployment to Deploy Tools

To deploy a new release of Inspector Lambda, you may first want to update the value of `softwareVersion` in **build.sbt** as well as the corresponding values in **scripts/jar-upload-to-s3.sh** and **scripts/lambda-function-update.sh**. Then,  make sure you have **deployTools** credentials and run

```
./jar-upload-to-s3.sh
```

### Team release

To upgrade your team's lambda function, make sure you have the correct Janus credentials and run 

```
./lambda-function-update.sh <aws-account-name>
```

