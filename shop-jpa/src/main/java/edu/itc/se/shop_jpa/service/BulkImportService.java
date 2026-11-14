package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkImportService {

    private static final int CHUNK_SIZE = 500;

    private final SessionFactory sessionFactory;
    private final ProductRepository products;
    private final EntityManager entityManager;

    public BulkImportService(
            jakarta.persistence.EntityManagerFactory emf,
            ProductRepository products,
            EntityManager entityManager) {
        this.sessionFactory = emf.unwrap(SessionFactory.class);
        this.products = products;
        this.entityManager = entityManager;
    }

    public int importCsv(Path csv) throws IOException {
        int imported = 0;
        try (StatelessSession session = sessionFactory.openStatelessSession();
             Stream<String> lines = Files.lines(csv).skip(1)) {
            Transaction transaction = session.beginTransaction();
            session.setJdbcBatchSize(CHUNK_SIZE);
            List<Product> chunk = new ArrayList<>(CHUNK_SIZE);
            try {
                var iterator = lines.iterator();
                while (iterator.hasNext()) {
                    chunk.add(parse(iterator.next(), imported + chunk.size() + 2));
                    if (chunk.size() == CHUNK_SIZE) {
                        session.insertMultiple(chunk);
                        imported += chunk.size();
                        chunk.clear();
                    }
                }
                if (!chunk.isEmpty()) {
                    session.insertMultiple(chunk);
                    imported += chunk.size();
                }
                transaction.commit();
            } catch (RuntimeException ex) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw ex;
            }
        }
        return imported;
    }

    @Transactional(rollbackFor = IOException.class)
    public int importWithSaveAll(Path csv, boolean clearEveryChunk) throws IOException {
        int imported = 0;
        List<Product> chunk = new ArrayList<>(CHUNK_SIZE);
        try (Stream<String> lines = Files.lines(csv).skip(1)) {
            var iterator = lines.iterator();
            while (iterator.hasNext()) {
                chunk.add(parse(iterator.next(), imported + chunk.size() + 2));
                if (chunk.size() == CHUNK_SIZE) {
                    products.saveAll(chunk);
                    imported += chunk.size();
                    chunk.clear();
                    if (clearEveryChunk) {
                        products.flush();
                        entityManager.clear();
                    }
                }
            }
        }
        if (!chunk.isEmpty()) {
            products.saveAll(chunk);
            imported += chunk.size();
        }
        return imported;
    }

    private static Product parse(String line, int lineNumber) {
        String[] fields = line.split(";", -1);
        if (fields.length != 6) {
            throw new IllegalArgumentException("Invalid supplier CSV at line " + lineNumber + ": expected 6 fields");
        }
        try {
            return new Product(
                    fields[0].trim(),
                    fields[1].trim(),
                    new Money(new BigDecimal(fields[2].trim()), fields[3].trim()),
                    Category.valueOf(fields[4].trim()),
                    Integer.parseInt(fields[5].trim()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid supplier CSV value at line " + lineNumber, ex);
        }
    }
}
