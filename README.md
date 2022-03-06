# GenCaster

An web environment for generative audio streams with low latency and device agnostic thanks to WebRTC.

## Services

Service | Comment
--- | ---
CasterBack | Django backend for management of streams
CasterFront | Vue Frontend for user interaction
CasterSound | Janus server for WebRTC streams of SuperCollider audio

## Development

Please use [`pre-commit`](https://pre-commit.com/) before committing to the repository.

The services can be started with `docker-compose up`.
