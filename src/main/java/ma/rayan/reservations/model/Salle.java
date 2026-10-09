package ma.rayan.reservations.model;

import javax.persistence.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "salles")
public class Salle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String nom;
    @Column(nullable = false)
    private int capacite;
    @OneToMany(mappedBy = "salle")
    private Set<Reservation> reservations = new LinkedHashSet<>();
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "salle_equipement",
        joinColumns = @JoinColumn(name = "salle_id"),
        inverseJoinColumns = @JoinColumn(name = "equipement_id"))
    private Set<Equipement> equipements = new LinkedHashSet<>();

    public Salle() {}
    public Salle(String nom, int capacite) { this.nom = nom; this.capacite = capacite; }
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }
    public Set<Reservation> getReservations() { return Collections.unmodifiableSet(reservations); }
    public Set<Equipement> getEquipements() { return Collections.unmodifiableSet(equipements); }

    public void ajouterReservation(Reservation reservation) { reservation.setSalle(this); }
    void associerReservation(Reservation reservation) { reservations.add(reservation); }
    void dissocierReservation(Reservation reservation) { reservations.remove(reservation); }

    public void ajouterEquipement(Equipement equipement) {
        equipements.add(equipement);
        equipement.associerSalle(this);
    }
    public void retirerEquipement(Equipement equipement) {
        equipements.remove(equipement);
        equipement.dissocierSalle(this);
    }

    @Override
    public String toString() { return "Salle{id=" + id + ", nom='" + nom + "', capacite=" + capacite + "}"; }
}
