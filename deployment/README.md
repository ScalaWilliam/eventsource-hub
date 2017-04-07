# Deployment to AWS

```
$ aws cloudformation create-stack \
    --region eu-west-1 \
    --stack-name sample-stack \
    --template-body file://$PWD/deployment/cloudformation.yml
{
    "StackId": "arn:aws:cloudformation:eu-west-1:128853988345:stack/sample-stack/14b0e410-1b22-11e7-8de5-500c423d20d2"
}
$ aws cloudformation describe-stack-events --region eu-west-1 --stack-name sample-stack
```
