# Punteggio – tennis e padel per Wear OS

App per smartwatch Wear OS (pensata per il OnePlus Watch 2R) per tenere il punteggio mentre si gioca a tennis o a padel.

## Cosa fa

- Si apre direttamente sul punteggio 0-0, senza impostazioni.
- Punti 0 / 15 / 30 / 40 / AD, game, set e **tie-break** a 7 sul 6-6, con la rotazione corretta del servizio. I set non hanno limite: si gioca finché vuoi.
- Indicatore di chi serve (pallina gialla).
- La partita viene salvata a ogni punto: se l'app si chiude, riprende da dove eri.
- Lo schermo resta acceso durante la partita e, se l'orologio si blocca, il punteggio compare sopra il blocco schermo: non serve inserire il PIN tra un punto e l'altro.

## Come si usa

- Metà alta (blu) = **LORO**, metà bassa (verde) = **NOI**.
- In ogni metà: tocca a **destra** per aggiungere un punto, a **sinistra** per toglierlo.
- **Scorri verso l'alto** per dare il servizio a loro, **verso il basso** per darlo a noi. La pallina gialla indica chi serve e da lì la rotazione continua da sola.
- **Scorri a destra** per trasformare il game in tie-break (punti 1, 2, 3… fino a 7, chi vince prende il set), **a sinistra** per tornare a 15/30/40. Lo swipe a destra quindi non chiude l'app: per uscire usa il tasto dell'orologio.
- Al centro vedi set e game. Il pulsante **↺** azzera il punteggio, dopo averti chiesto conferma.

## Struttura

- `core/` – logica del punteggio in Kotlin puro, con test (`./gradlew :core:test`).
- `wear/` – app Wear OS in Jetpack Compose.

## Compilare e installare sull'orologio

Serve [Android Studio](https://developer.android.com/studio) (oppure scarica l'APK già compilato dalla scheda **Actions** del repo, artefatto `punteggio-debug-apk`).

1. Sull'orologio (OnePlus Watch 2R): Impostazioni → Altro → Informazioni sull'orologio → Altre informazioni versione → tocca 7 volte "Numero build" per attivare le Opzioni sviluppatore.
2. Opzioni sviluppatore → attiva **Debug ADB** e **Debug wireless**. Orologio e computer devono essere sulla stessa rete Wi-Fi.
3. In Debug wireless tocca "Associa nuovo dispositivo" e sul computer:
   ```
   adb pair <ip>:<porta-associazione>   # inserisci il codice mostrato sull'orologio
   adb connect <ip>:<porta>
   ```
4. Da Android Studio premi Run scegliendo l'orologio, oppure:
   ```
   ./gradlew :wear:installDebug
   # o con l'APK scaricato:
   adb install wear-debug.apk
   ```
