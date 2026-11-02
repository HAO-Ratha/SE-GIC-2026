package edu.itc.se.shop.customer;

import edu.itc.se.shop.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public Customer get(long id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer " + id + " not found"));
    }
}