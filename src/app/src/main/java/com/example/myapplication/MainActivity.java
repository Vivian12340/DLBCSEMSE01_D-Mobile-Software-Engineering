package com.example.boardgameapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
 * MainActivity
 *
 * Diese Activity bildet die Startseite der App.
 * Hier sieht der Benutzer die wichtigsten Informationen und Funktionen auf einen Blick.
 *
 * Aktuell wird noch keine Datenbank verwendet.
 * Deshalb werden Daten nur innerhalb des aktuellen Ablaufs verarbeitet.
 */
public class MainActivity extends AppCompatActivity {

    private MaterialButton btnChat;
    private Button btnRateEvening;
    private MaterialButton btnSettings;
    private MaterialButton btnNewGame;
    private TextView btnAddDate;

    // Aktive Termin-Card
    private MaterialCardView cardActiveGame;

    // TextViews der aktiven Termin-Card
    private TextView tvHostActive;
    private TextView tvDateActive;
    private TextView tvTimeActive;
    private TextView tvLocationActive;
    private TextView tvGamesActive;

    // Merkt sich, ob ein echter aktiver Termin erstellt wurde
    private boolean hasActiveTerm = false;

    // Speichert das aktuelle Termin-Datum als String
    private String currentDateTime = "";

    /*
     * Launcher für NewDateActivity
     *
     * Empfängt die Daten des neu erstellten Termins zurück.
     */
    private final ActivityResultLauncher<Intent> newDateLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                            String host = result.getData().getStringExtra("host");
                            String dateTime = result.getData().getStringExtra("dateTime");
                            String location = result.getData().getStringExtra("location");

                            if (host != null && dateTime != null && location != null) {
                                showActiveDate(host, dateTime, location);
                            }
                        }
                    }
            );

    /*
     * Launcher für VotingActivity
     *
     * Aktuell wird hier noch nichts zurückverarbeitet,
     * weil die echten Voting-Ergebnisse ohne Datenbank noch nicht sauber berechnet werden können.
     */
    private final ActivityResultLauncher<Intent> votingLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        // Platz für spätere Ergebnisverarbeitung
                    }
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verbindet diese Activity mit dem Layout der Hauptseite
        setContentView(R.layout.activity_main);

        // =========================
        // Buttons und Views verbinden
        // =========================
        btnChat = findViewById(R.id.btnChat);
        btnRateEvening = findViewById(R.id.btnRateEvening);
        btnSettings = findViewById(R.id.btnSettings);
        btnNewGame = findViewById(R.id.btnNewGame);
        btnAddDate = findViewById(R.id.btnAddDate);

        cardActiveGame = findViewById(R.id.cardActiveGame);

        tvHostActive = findViewById(R.id.tvHostActive);
        tvDateActive = findViewById(R.id.tvDateActive);
        tvTimeActive = findViewById(R.id.tvTimeActive);
        tvLocationActive = findViewById(R.id.tvLocationActive);
        tvGamesActive = findViewById(R.id.tvGamesActive);

        // =========================
        // Chat öffnen
        // =========================
        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        // =========================
        // Bewertungsdialog öffnen
        // =========================
        btnRateEvening.setOnClickListener(v -> {
            BewertungDialogFragment dialog = new BewertungDialogFragment();
            dialog.show(getSupportFragmentManager(), "BewertungDialog");
        });

        // =========================
        // Settings öffnen
        // =========================
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // =========================
        // NewGameActivity öffnen
        // =========================
        btnNewGame.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewGameActivity.class);
            startActivity(intent);
        });

        // =========================
        // NewDateActivity öffnen
        // =========================
        btnAddDate.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewDateActivity.class);
            newDateLauncher.launch(intent);
        });

        // =========================
        // Klick auf aktiven Termin öffnet Voting
        // Nur wenn ein echter aktiver Termin existiert
        // und das Voting noch erlaubt ist
        // =========================
        cardActiveGame.setOnClickListener(v -> openVotingIfAllowed());
    }

    /*
     * showActiveDate
     *
     * Zeigt den neu erstellten aktiven Termin in der MainActivity an.
     */
    private void showActiveDate(String host, String dateTime, String location) {
        hasActiveTerm = true;
        currentDateTime = dateTime;

        // Host anzeigen
        tvHostActive.setText(host);

        // Datum und Uhrzeit trennen
        int lastSpaceIndex = dateTime.lastIndexOf(" ");
        if (lastSpaceIndex != -1) {
            String datePart = dateTime.substring(0, lastSpaceIndex);
            String timePart = dateTime.substring(lastSpaceIndex + 1);

            tvDateActive.setText(datePart);
            tvTimeActive.setText(timePart);
        } else {
            tvDateActive.setText(dateTime);
            tvTimeActive.setText("");
        }

        // Ort anzeigen
        tvLocationActive.setText(location);

        // Spieletext zurücksetzen
        tvGamesActive.setText("Noch keine Spiele ausgewählt");
    }

    /*
     * openVotingIfAllowed
     *
     * Öffnet die VotingActivity nur dann,
     * wenn ein aktiver Termin existiert und das Voting zeitlich noch erlaubt ist.
     */
    private void openVotingIfAllowed() {

        if (!hasActiveTerm) {
            Toast.makeText(this, "Bitte zuerst einen Termin erstellen", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isVotingStillAllowed(currentDateTime)) {
            Toast.makeText(this,
                    "Abstimmen ist nur bis 1 Tag vor dem Termin möglich",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, VotingActivity.class);
        intent.putExtra("dateTime", currentDateTime);
        votingLauncher.launch(intent);
    }

    /*
     * isVotingStillAllowed
     *
     * Prüft, ob der Termin mehr als 24 Stunden in der Zukunft liegt.
     * Nur dann darf noch abgestimmt werden.
     */
    private boolean isVotingStillAllowed(String dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        try {
            Date date = sdf.parse(dateTime);
            if (date == null) {
                return false;
            }

            long nowMillis = System.currentTimeMillis();
            long termMillis = date.getTime();

            long diffMillis = termMillis - nowMillis;
            long oneDayMillis = 24L * 60L * 60L * 1000L;

            return diffMillis > oneDayMillis;

        } catch (ParseException e) {
            return false;
        }
    }
}
