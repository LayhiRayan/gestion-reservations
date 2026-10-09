package ma.rayan.reservations;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;
import ma.rayan.reservations.model.*;
import ma.rayan.reservations.service.ReservationService;
import org.junit.*;
import static org.junit.Assert.*;

public class RelationsJpaTest {
    private ReservationService service;
    private final LocalDateTime debut = LocalDateTime.of(2026, 11, 12, 9, 0);

    @Before
    public void preparer() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("javax.persistence.jdbc.url", "jdbc:h2:mem:test_" + UUID.randomUUID());
        configuration.put("hibernate.hbm2ddl.auto", "create-drop");
        configuration.put("hibernate.show_sql", "false");
        service = new ReservationService(configuration);
    }
    @After
    public void fermer() { if (service != null) { service.close(); } }

    private Long[] creerGraphe() {
        return service.transaction(em -> {
            Salle salle = new Salle("Atlas", 30);
            Equipement equipement = new Equipement("Projecteur");
            salle.ajouterEquipement(equipement);
            Utilisateur utilisateur = new Utilisateur("Rayan", "rayan@example.com");
            Reservation reservation = new Reservation(debut, debut.plusHours(1), salle);
            utilisateur.ajouterReservation(reservation);
            em.persist(utilisateur);
            return new Long[]{utilisateur.getId(), reservation.getId(), salle.getId(), equipement.getId()};
        });
    }

    private long compter(EntityManager em, String entite) {
        return em.createQuery("SELECT COUNT(e) FROM " + entite + " e", Long.class).getSingleResult();
    }

    @Test
    public void persisterToutLeGrapheDepuisUtilisateur() {
        Long[] ids = creerGraphe();
        service.transaction(em -> {
            for (String type : new String[]{"Utilisateur", "Reservation", "Salle", "Equipement"}) {
                assertEquals(1, compter(em, type));
            }
            Reservation reservation = em.find(Reservation.class, ids[1]);
            assertEquals(ids[0], reservation.getUtilisateur().getId());
            assertEquals(ids[2], reservation.getSalle().getId());
            assertEquals(1, reservation.getSalle().getReservations().size());
            assertEquals(1, em.find(Equipement.class, ids[3]).getSalles().size());
            return null;
        });
    }

    @Test
    public void supprimerOrphelinSansSupprimerSalleEtEquipement() {
        Long[] ids = creerGraphe();
        service.annulerReservation(ids[1]);
        service.transaction(em -> {
            assertNull(em.find(Reservation.class, ids[1]));
            assertTrue(em.find(Utilisateur.class, ids[0]).getReservations().isEmpty());
            assertTrue(em.find(Salle.class, ids[2]).getReservations().isEmpty());
            assertNotNull(em.find(Equipement.class, ids[3]));
            return null;
        });
    }

    @Test
    public void supprimerUtilisateurSansToucherReservationAutrui() {
        Long[] ids = creerGraphe();
        Long autreId = service.transaction(em -> {
            Utilisateur autre = new Utilisateur("Salma", "salma@example.com");
            autre.ajouterReservation(new Reservation(debut.plusHours(2), debut.plusHours(3), em.find(Salle.class, ids[2])));
            em.persist(autre);
            return autre.getId();
        });
        service.supprimerUtilisateur(ids[0]);
        assertEquals(1, service.reservationsUtilisateur(autreId).size());
        service.transaction(em -> {
            assertNull(em.find(Utilisateur.class, ids[0]));
            assertNull(em.find(Reservation.class, ids[1]));
            assertNotNull(em.find(Salle.class, ids[2]));
            assertNotNull(em.find(Equipement.class, ids[3]));
            return null;
        });
    }

    @Test
    public void partagerPuisRetirerEquipementEtSupprimerSalle() {
        Long[] ids = creerGraphe();
        Long autreSalle = service.transaction(em -> {
            Salle salle = new Salle("Menara", 16);
            salle.ajouterEquipement(em.find(Equipement.class, ids[3]));
            em.persist(salle);
            return salle.getId();
        });
        assertEquals(2, service.sallesAvecEquipement("Projecteur").size());
        service.transaction(em -> {
            Salle salle = em.find(Salle.class, ids[2]);
            Equipement equipement = em.find(Equipement.class, ids[3]);
            salle.retirerEquipement(equipement);
            assertFalse(equipement.getSalles().contains(salle));
            return null;
        });
        assertEquals(1, service.sallesAvecEquipement("Projecteur").size());
        service.transaction(em -> { em.remove(em.find(Salle.class, autreSalle)); return null; });
        service.transaction(em -> {
            assertNotNull(em.find(Equipement.class, ids[3]));
            assertTrue(em.find(Equipement.class, ids[3]).getSalles().isEmpty());
            return null;
        });
    }

    @Test
    public void verifierDisponibiliteEtRefuserChevauchement() {
        Long[] ids = creerGraphe();
        assertFalse(service.estDisponible(ids[2], debut, debut.plusHours(1)));
        assertFalse(service.estDisponible(ids[2], debut.minusHours(1), debut.plusHours(2)));
        assertFalse(service.estDisponible(ids[2], debut.plusMinutes(15), debut.plusMinutes(45)));
        assertTrue(service.estDisponible(ids[2], debut.plusHours(1), debut.plusHours(2)));
        assertTrue(service.estDisponible(ids[2], debut.minusHours(1), debut));
        assertThrows(IllegalStateException.class, () -> service.reserver(ids[0], ids[2], debut, debut.plusHours(1)));
        service.reserver(ids[0], ids[2], debut.plusHours(1), debut.plusHours(2));
        assertEquals(2, service.reservationsUtilisateur(ids[0]).size());
    }

    @Test
    public void refuserDatesInvalidesEtIdentifiantsInconnus() {
        assertThrows(IllegalArgumentException.class, () -> new Reservation(debut, debut, null));
        assertThrows(IllegalArgumentException.class, () -> new Reservation(debut, debut.minusHours(1), null));
        assertThrows(IllegalArgumentException.class, () -> new Reservation(null, debut, null));
        assertThrows(IllegalArgumentException.class, () -> service.estDisponible(-1L, debut, debut.plusHours(1)));
        assertThrows(IllegalArgumentException.class, () -> service.reserver(-1L, -1L, debut, debut.plusHours(1)));
    }

    @Test
    public void annulerTransactionEntiereApresViolationUnicite() {
        creerGraphe();
        assertThrows(RuntimeException.class, () -> service.transaction(em -> {
            em.persist(new Salle("Menara", 16));
            em.persist(new Utilisateur("Doublon", "rayan@example.com"));
            return null;
        }));
        service.transaction(em -> {
            assertEquals(1, compter(em, "Salle"));
            assertEquals(1, compter(em, "Utilisateur"));
            em.persist(new Salle("Toubkal", 40));
            return null;
        });
    }

    @Test
    public void propagerMergeVersReservationSalleEtEquipement() {
        Long[] ids = creerGraphe();
        Utilisateur detache = service.transaction(em -> {
            Utilisateur utilisateur = em.find(Utilisateur.class, ids[0]);
            for (Reservation reservation : utilisateur.getReservations()) {
                reservation.getSalle().getEquipements().size();
            }
            return utilisateur;
        });
        Reservation reservation = detache.getReservations().iterator().next();
        reservation.modifierPeriode(debut.plusHours(2), debut.plusHours(3));
        reservation.getSalle().setCapacite(45);
        reservation.getSalle().getEquipements().iterator().next().setNom("Projecteur HD");
        service.transaction(em -> { em.merge(detache); return null; });
        service.transaction(em -> {
            assertEquals(debut.plusHours(2), em.find(Reservation.class, ids[1]).getDebut());
            assertEquals(45, em.find(Salle.class, ids[2]).getCapacite());
            assertEquals("Projecteur HD", em.find(Equipement.class, ids[3]).getNom());
            return null;
        });
    }

    @Test
    public void synchroniserAssociationsEnMemoire() {
        Salle atlas = new Salle("Atlas", 30);
        Salle menara = new Salle("Menara", 16);
        Utilisateur utilisateur = new Utilisateur("Rayan", "rayan@example.com");
        Reservation reservation = new Reservation(debut, debut.plusHours(1), atlas);
        utilisateur.ajouterReservation(reservation);
        reservation.setSalle(menara);
        assertTrue(atlas.getReservations().isEmpty());
        assertTrue(menara.getReservations().contains(reservation));
        utilisateur.retirerReservation(reservation);
        assertNull(reservation.getUtilisateur());
        assertNull(reservation.getSalle());
        assertTrue(menara.getReservations().isEmpty());
    }
}
