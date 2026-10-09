package ma.rayan.reservations;

import java.time.LocalDateTime;
import java.util.Arrays;
import ma.rayan.reservations.model.*;
import ma.rayan.reservations.service.ReservationService;
import org.h2.tools.Server;

public class ApplicationReservations {
    public static void main(String[] args) throws Exception {
        try (ReservationService service = new ReservationService()) {
            LocalDateTime matin = LocalDateTime.of(2026, 11, 12, 9, 0);
            Long[] ids = service.transaction(em -> {
                Equipement projecteur = new Equipement("Vidéoprojecteur");
                Salle atlas = new Salle("Atlas", 30);
                Salle menara = new Salle("Menara", 16);
                atlas.ajouterEquipement(projecteur);
                atlas.ajouterEquipement(new Equipement("Tableau interactif"));
                menara.ajouterEquipement(projecteur);
                Utilisateur rayan = new Utilisateur("Rayan", "rayan@example.com");
                Reservation reunion = new Reservation(matin, matin.plusHours(1), atlas);
                rayan.ajouterReservation(reunion);
                rayan.ajouterReservation(new Reservation(matin.plusHours(2), matin.plusHours(3), menara));
                em.persist(rayan);
                Utilisateur salma = new Utilisateur("Salma", "salma@example.com");
                salma.ajouterReservation(new Reservation(matin.plusHours(4), matin.plusHours(5), atlas));
                em.persist(salma);
                return new Long[]{rayan.getId(), salma.getId(), reunion.getId(), atlas.getId(), menara.getId()};
            });
            System.out.println("\n1. Graphe enregistré par cascade :");
            service.reservationsUtilisateur(ids[0]).forEach(System.out::println);
            System.out.println("\n2. Salles avec vidéoprojecteur partagé :");
            service.sallesAvecEquipement("Vidéoprojecteur").forEach(System.out::println);
            System.out.println("\n3. Atlas disponible à 9 h : " + service.estDisponible(ids[3], matin, matin.plusHours(1)));
            System.out.println("Atlas disponible à 10 h : " + service.estDisponible(ids[3], matin.plusHours(1), matin.plusHours(2)));
            service.transaction(em -> {
                em.find(Salle.class, ids[4]).setCapacite(20);
                return null;
            });
            System.out.println("\n4. Capacité de Menara modifiée à 20 places.");
            service.annulerReservation(ids[2]);
            System.out.println("\n5. Réservation retirée : suppression automatique de l'orphelin.");
            service.reservationsUtilisateur(ids[0]).forEach(System.out::println);
            service.supprimerUtilisateur(ids[0]);
            System.out.println("\n6. Rayan supprimé avec ses réservations. Salma conserve la sienne :");
            service.reservationsUtilisateur(ids[1]).forEach(System.out::println);
            System.out.println("\nSalles et équipements partagés conservés :");
            service.sallesAvecEquipement("Vidéoprojecteur").forEach(System.out::println);
            if (Arrays.asList(args).contains("--console")) {
                Server console = Server.createWebServer("-webPort", "8082").start();
                try {
                    System.out.println("\nConsole H2 : http://localhost:8082");
                    System.out.println("JDBC : jdbc:h2:mem:reservations | Utilisateur : sa | Mot de passe vide");
                    System.out.println("Appuyez sur Entrée pour arrêter.");
                    System.in.read();
                } finally { console.stop(); }
            }
        }
    }
}
