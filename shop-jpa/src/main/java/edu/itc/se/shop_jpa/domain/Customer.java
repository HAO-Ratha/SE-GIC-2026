package edu.itc.se.shop_jpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "customer")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
    @SequenceGenerator(name = "customer_seq", sequenceName = "customer_seq", allocationSize = 50)
    private Long id;

    @NotBlank
    private String name;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "street", nullable = false)),
        @AttributeOverride(name = "city", column = @Column(name = "city", nullable = false, length = 100)),
        @AttributeOverride(name = "zip", column = @Column(name = "zip", nullable = false, length = 10))
    })
    private Address address;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "ship_street", nullable = false)),
        @AttributeOverride(name = "city", column = @Column(name = "ship_city", nullable = false, length = 100)),
        @AttributeOverride(name = "zip", column = @Column(name = "ship_zip", nullable = false, length = 10))
    })
    private Address shippingAddress;

    protected Customer() {}

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Customer(String name, String email, Address address, Address shippingAddress) {
        this.name = name;
        this.email = email;
        this.address = address;
        this.shippingAddress = shippingAddress;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Address getAddress() { return address; }
    public Address getShippingAddress() { return shippingAddress; }
}