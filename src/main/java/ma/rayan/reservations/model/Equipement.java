package ma.rayan.reservations.model;

import javax.persistence.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "equipements")
public class Equipement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String nom;
    @ManyToMany(mappedBy = "equipements")
    private Set<Salle> salles = new LinkedHashSet<>();

    public Equipement() {}
    public Equipement(String nom) { this.nom = nom; }
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public Set<Salle> getSalles() { return Collections.unmodifiableSet(salles); }
    public void ajouterSalle(Salle salle) { salle.ajouterEquipement(this); }
    public void retirerSalle(Salle salle) { salle.retirerEquipement(this); }
    void associerSalle(Salle salle) { salles.add(salle); }
    void dissocierSalle(Salle salle) { salles.remove(salle); }

    @Override
    public String toString() { return "Equipement{id=" + id + ", nom='" + nom + "'}"; }
}
