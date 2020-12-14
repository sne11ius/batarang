# batarang

A cli tool to easen the burden of everyday dev tasks

## Up next

### CI prerequisites

- Allow settings-overrides via ENV
- Use settings-overrides & dummy server to generate native-image configurations
  during integration build

### CI

- https://blogs.oracle.com/developers/building-cross-platform-native-images-with-graalvm
- include upx in builds
- final executable shoud be `bat`

### Code quality

- Refactor
- Integrate ktlint & detekt

### Promo

- Record demo sessions via https://asciinema.org/

## What

### Bitbucket

- Clone
- Browse

### Jenkins

- Run
- Browse

### More

- Add more usefull stuff
- Private Modules (like shell scripts)
- Disable/Enable Modules

## Build

### Prerequsites

- graalvm 20.3.0 (openjdk 11)
- Build and local install the latest `release/3.1` branch of lanterna

### Build batarang

see `rebuild.sh`

### My native executable is 49 MB wth?

Use upx to reduce the size down to 14 MB

## License

EUPL-1.2

- See [LICENSE](LICENSE)
- See https://joinup.ec.europa.eu/collection/eupl
