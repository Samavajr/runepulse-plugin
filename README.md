# RunePulse Plugin

RunePulse is a RuneLite plugin that sends **opt-in** telemetry (XP, gear snapshots, boss KC) to the RunePulse backend.

## Data collected
- In-game username
- XP changes and baselines
- Equipped gear snapshots
- Boss kill count messages from chat

**Not collected:** email, password, or any login credentials.

## Requirements
- RuneLite (official client)
- Java 11

## Build
```
./gradlew build
```

## Config
This plugin uses a "Pair Now" button to register a token. No manual API key needed.

