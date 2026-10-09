package ma.rayan.reservations.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalDateTime debut;
    @Column(nullable = false)
    private LocalDateTime fin;
    @ManyToOne(optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
    // Persister le graphe ne doit jamais supprimer une salle partagée.
    @ManyToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    public Reservation() {}
    public Reservation(LocalDateTime debut, LocalDateTime fin, Salle salle) {
        modifierPeriode(debut, fin);
        setSalle(salle);
    }
    public Long getId() { return id; }
    public LocalDateTime getDebut() { return debut; }
    public LocalDateTime getFin() { return fin; }
    public Salle getSalle() { return salle; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    void associerUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }

    public void setSalle(Salle nouvelleSalle) {
        if (salle == nouvelleSalle) { return; }
        if (salle != null) { salle.dissocierReservation(this); }
        salle = nouvelleSalle;
        if (salle != null) { salle.associerReservation(this); }
    }

    public void modifierPeriode(LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin);
        this.debut = debut;
        this.fin = fin;
    }
    public static void verifierPeriode(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null || !debut.isBefore(fin)) {
            throw new IllegalArgumentException("La date de début doit précéder la date de fin.");
        }
    }
    @PrePersist
    @PreUpdate
    private void valider() { verifierPeriode(debut, fin); }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", debut=" + debut + ", fin=" + fin
                + ", salle=" + (salle == null ? "aucune" : salle.getNom())
                + ", utilisateur=" + (utilisateur == null ? "aucun" : utilisateur.getNom()) + "}";
    }
}
