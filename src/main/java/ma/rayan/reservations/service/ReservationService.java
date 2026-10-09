package ma.rayan.reservations.service;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import ma.rayan.reservations.model.*;

public class ReservationService implements AutoCloseable {
    private final EntityManagerFactory fabrique;
    public ReservationService() { this(Collections.emptyMap()); }
    public ReservationService(Map<String, ?> configuration) {
        fabrique = Persistence.createEntityManagerFactory("reservationsPU", configuration);
    }

    public <T> T transaction(Function<EntityManager, T> operation) {
        EntityManager em = fabrique.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T resultat = operation.apply(em);
            tx.commit();
            return resultat;
        } catch (RuntimeException erreur) {
            try { if (tx.isActive()) { tx.rollback(); } }
            catch (RuntimeException annulation) { erreur.addSuppressed(annulation); }
            throw erreur;
        } finally {
            em.close();
        }
    }

    public boolean estDisponible(Long salleId, LocalDateTime debut, LocalDateTime fin) {
        Reservation.verifierPeriode(debut, fin);
        return transaction(em -> {
            if (em.find(Salle.class, salleId) == null) { throw new IllegalArgumentException("Salle inconnue."); }
            return sansChevauchement(em, salleId, debut, fin);
        });
    }

    private boolean sansChevauchement(EntityManager em, Long salleId, LocalDateTime debut, LocalDateTime fin) {
        return em.createQuery("SELECT COUNT(r) FROM Reservation r WHERE r.salle.id = :salle "
                + "AND r.debut < :fin AND r.fin > :debut", Long.class)
                .setParameter("salle", salleId).setParameter("debut", debut).setParameter("fin", fin)
                .getSingleResult() == 0;
    }

    public Long reserver(Long utilisateurId, Long salleId, LocalDateTime debut, LocalDateTime fin) {
        Reservation.verifierPeriode(debut, fin);
        return transaction(em -> {
            Salle salle = em.find(Salle.class, salleId, LockModeType.PESSIMISTIC_WRITE);
            Utilisateur utilisateur = em.find(Utilisateur.class, utilisateurId);
            if (salle == null || utilisateur == null) { throw new IllegalArgumentException("Salle ou utilisateur inconnu."); }
            if (!sansChevauchement(em, salleId, debut, fin)) { throw new IllegalStateException("Salle déjà réservée sur cette période."); }
            Reservation reservation = new Reservation(debut, fin, salle);
            utilisateur.ajouterReservation(reservation);
            em.persist(reservation);
            return reservation.getId();
        });
    }

    public List<Reservation> reservationsUtilisateur(Long utilisateurId) {
        return transaction(em -> em.createQuery(
                "SELECT r FROM Reservation r JOIN FETCH r.salle JOIN FETCH r.utilisateur "
                + "WHERE r.utilisateur.id = :id ORDER BY r.debut", Reservation.class)
                .setParameter("id", utilisateurId).getResultList());
    }

    public List<Salle> sallesAvecEquipement(String nom) {
        return transaction(em -> em.createQuery(
                "SELECT DISTINCT s FROM Salle s JOIN s.equipements e WHERE e.nom = :nom ORDER BY s.nom", Salle.class)
                .setParameter("nom", nom).getResultList());
    }

    public void annulerReservation(Long id) {
        transaction(em -> {
            Reservation reservation = em.find(Reservation.class, id);
            if (reservation == null) { throw new IllegalArgumentException("Réservation inconnue."); }
            reservation.getUtilisateur().retirerReservation(reservation);
            return null;
        });
    }

    public void supprimerUtilisateur(Long id) {
        transaction(em -> {
            Utilisateur utilisateur = em.find(Utilisateur.class, id);
            if (utilisateur == null) { throw new IllegalArgumentException("Utilisateur inconnu."); }
            // Nettoyer aussi le côté inverse en mémoire avant la suppression en cascade.
            for (Reservation reservation : utilisateur.getReservations()) { reservation.setSalle(null); }
            em.remove(utilisateur);
            return null;
        });
    }

    @Override
    public void close() { if (fabrique.isOpen()) { fabrique.close(); } }
}
