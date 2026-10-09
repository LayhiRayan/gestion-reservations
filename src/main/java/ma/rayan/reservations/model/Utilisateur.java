package ma.rayan.reservations.model;

import javax.persistence.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "utilisateurs")
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nom;
    @Column(nullable = false, unique = true)
    private String email;
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Reservation> reservations = new LinkedHashSet<>();

    public Utilisateur() {}
    public Utilisateur(String nom, String email) { this.nom = nom; this.email = email; }
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Set<Reservation> getReservations() { return Collections.unmodifiableSet(reservations); }

    public void ajouterReservation(Reservation reservation) {
        if (reservation.getUtilisateur() != null && reservation.getUtilisateur() != this) {
            throw new IllegalArgumentException("Une réservation appartient déjà à un autre utilisateur.");
        }
        reservations.add(reservation);
        reservation.associerUtilisateur(this);
    }

    public void retirerReservation(Reservation reservation) {
        if (reservations.remove(reservation)) {
            reservation.associerUtilisateur(null);
            reservation.setSalle(null);
        }
    }

    @Override
    public String toString() { return "Utilisateur{id=" + id + ", nom='" + nom + "', email='" + email + "'}"; }
}
