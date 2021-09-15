# File Flow Analysis - Analyzer

https://hub.docker.com/r/rodneyxr/ffa-analyzer

```sh
mkdir ffa && cd ffa
echo "touch 'hello_world';" > test.ffa
docker run --rm -v $(pwd):/ffa docker.io/rodneyxr/ffa-analyzer:latest
```