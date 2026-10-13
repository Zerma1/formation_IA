Big Pickle · 18.7s · 107.4 tok/s
>tu as le prompte dans prompte2.md, lit le et applique ce qui est demander

Thought · 31.5s
Understood — I'll apply prompte2.md now. Let me note the one environment constraint I found: Docker is not installed on this machine, so the Testcontainers integration tests must skip gracefully while still compiling; unit and web tests will run. I'll build with PostgreSQL + Testcontainers as required.
Let me start with the Maven build, then verify the exact Testcontainers 2.0.5 API before writing tests.
pom.xml
```.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
<modelVersion>4.0.0</modelVersion>
<parent>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-parent</artifactId>
<version>4.1.1</version>
<relativePath/> <!-- lookup parent from repository -->
</parent>
<groupId>com.example</groupId>
<artifactId>ia-spring-lab</artifactId>
<version>0.0.1-SNAPSHOT</version>
<name>ia-spring-lab</name>
<description>Order management REST API</description>
<url/>
<licenses>
<license/>
</licenses>
<developers>
<developer/>
</developers>
<scm>
<connection/>
<developerConnection/>
<tag/>
<url/>
</scm>
<properties>
<java.version>21</java.version>
</properties>
<dependencies>
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-flyway</artifactId>
</dependency>

        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

Thought · 1.9s
$ .\mvnw.cmd -B -ntp -DskipTests dependency:go-offline 2>&1 | Select-Object -Last 40
(33 earlier lines)
[INFO] Resolved dependency: istack-commons-runtime-4.1.2.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  11.244 s
[INFO] Finished at: 2026-10-13T10:01:12+02:00
[INFO] ------------------------------------------------------------------------
Thought · 2.6s
Dependencies resolved. Now let me verify the exact Testcontainers 2.0.5 package/class names before writing integration tests.
$ $base="$env:USERPROFILE\.m2\repository\org\testcontainers"; Get-ChildItem "$base" -Directory | ForEach-Object { $_.Name }
echo "=== core classes (containers) ==="…
(27 earlier lines)
org/testcontainers/junit/jupiter/TestcontainersTestDescription.class
org/testcontainers/junit/jupiter/EnabledIfDockerAvailableCondition.class
org/testcontainers/junit/jupiter/EnabledIfDockerAvailable.class
org/testcontainers/junit/jupiter/TestcontainersExtension$StoreAdapter.class
org/testcontainers/junit/jupiter/TestcontainersExtension.class
org/testcontainers/junit/jupiter/DockerAvailableDetector.class
Thought · 1.9s
$ $base="$env:USERPROFILE\.m2\repository\org\testcontainers"; $tc="$base\testcontainers\2.0.5\testcontainers-2.0.5.jar"; $jj="$base\testcontainers-junit-jupiter\2.0.5\testcontainers-junit-jupiter-2.0.5.jar"; $pg="$base\testcontainers-postgresql\2.0.5\testcontainers-postgre…
(32 earlier lines)
protected void waitUntilContainerStarted();
public org.testcontainers.containers.JdbcDatabaseContainer withDatabaseName(java.lang.String);
public org.testcontainers.containers.JdbcDatabaseContainer withPassword(java.lang.String);
public org.testcontainers.containers.JdbcDatabaseContainer withUsername(java.lang.String);

Exited with code 1
Thought · 19.3s
Confirmed the Testcontainers 2.0.5 API. Now I'll write the application. Starting with the domain model.
src\main\java\com\example\iaspringlab\domain\Client.java
package com.example.iaspringlab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
* Customer of the shop.
*
* <p>A customer has a first name, a last name and an email that must be unique.</p>
*/
@Entity
@Table(name = "client", uniqueConstraints = @UniqueConstraint(name = "uk_client_email", columnNames = "email"))
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false)
    private String email;

    protected Client() {
        // required by JPA
    }

    public Client(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }
}

src\main\java\com\example\iaspringlab\domain\Product.java
package com.example.iaspringlab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
* Product that can be ordered.
*
* <p>A product has a reference, a name, a price and an active status. An inactive
* product cannot be added to an order.</p>
  */
  @Entity
  @Table(name = "product", uniqueConstraints = @UniqueConstraint(name = "uk_product_reference", columnNames = "reference"))
  public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "reference", nullable = false)
  private String reference;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "price", nullable = false, precision = 19, scale = 2)
  private BigDecimal price;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  protected Product() {
  // required by JPA
  }

  public Product(String reference, String name, BigDecimal price) {
  this.reference = reference;
  this.name = name;
  this.price = price;
  this.active = true;
  }

  public void activate() {
  this.active = true;
  }

  public void deactivate() {
  this.active = false;
  }

  public Long getId() {
  return id;
  }

  public String getReference() {
  return reference;
  }

  public String getName() {
  return name;
  }

  public BigDecimal getPrice() {
  return price;
  }

  public boolean isActive() {
  return active;
  }
  }

src\main\java\com\example\iaspringlab\domain\OrderStatus.java
package com.example.iaspringlab.domain;

/**
* Lifecycle of an order.
*
* <p>Only the following transitions are allowed (business rule):</p>
* <pre>
* DRAFT      -&gt; CONFIRMED
* DRAFT      -&gt; CANCELLED
* CONFIRMED  -&gt; CANCELLED
* </pre>
*/
public enum OrderStatus {
DRAFT,
CONFIRMED,
CANCELLED;

    /**
     * @return {@code true} when moving from this status to {@code target} is allowed.
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case DRAFT -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == CANCELLED;
            case CANCELLED -> false;
        };
    }
}

src\main\java\com\example\iaspringlab\domain\OrderLine.java
package com.example.iaspringlab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
* A single line of an {@link Order}.
*
* <p>The line contains at least one unit of a product and remembers the unit price
* of the product at the time the line was added (price snapshot).</p>
  */
  @Entity
  @Table(name = "order_line")
  public class OrderLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
  private BigDecimal unitPrice;

  protected OrderLine() {
  // required by JPA
  }

  public OrderLine(Order order, Product product, int quantity) {
  this.order = order;
  this.product = product;
  this.quantity = quantity;
  this.unitPrice = product.getPrice();
  }

  /**
    * @return quantity × unitPrice.
      */
      public BigDecimal getLineTotal() {
      return unitPrice.multiply(BigDecimal.valueOf(quantity));
      }

  public Long getId() {
  return id;
  }

  public Order getOrder() {
  return order;
  }

  public Product getProduct() {
  return product;
  }

  public int getQuantity() {
  return quantity;
  }

  public BigDecimal getUnitPrice() {
  return unitPrice;
  }
  }

src\main\java\com\example\iaspringlab\domain\Order.java
package com.example.iaspringlab.domain;

import com.example.iaspringlab.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
* Customer order.
*
* <p>An order belongs to exactly one customer, contains at least one line, has a
* strictly positive quantity per line and keeps a snapshot of the product price.
* The total is the sum of {@code quantity × unitPrice} over all lines.</p>
  */
  @Entity
  @Table(name = "orders")
  public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private OrderStatus status;

  @Column(name = "total", nullable = false, precision = 19, scale = 2)
  private BigDecimal total = BigDecimal.ZERO;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<OrderLine> lines = new ArrayList<>();

  protected Order() {
  // required by JPA
  }

  public Order(Client client) {
  this.client = client;
  this.status = OrderStatus.DRAFT;
  this.total = BigDecimal.ZERO;
  }

  /**
    * Adds a line for the given product and quantity, then recomputes the total.
    *
    * @throws BusinessRuleException when the quantity is not strictly positive.
      */
      public void addLine(Product product, int quantity) {
      if (quantity <= 0) {
      throw new BusinessRuleException("La quantité d'une ligne doit être strictement positive");
      }
      lines.add(new OrderLine(this, product, quantity));
      recalculateTotal();
      }

  /**
    * Moves the order to {@code target}.
    *
    * @throws BusinessRuleException when the transition is not allowed.
      */
      public void transitionTo(OrderStatus target) {
      if (!status.canTransitionTo(target)) {
      throw new BusinessRuleException(
      "Transition de statut interdite : " + status + " -> " + target);
      }
      this.status = target;
      }

  private void recalculateTotal() {
  this.total = lines.stream()
  .map(OrderLine::getLineTotal)
  .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public Long getId() {
  return id;
  }

  public Client getClient() {
  return client;
  }

  public OrderStatus getStatus() {
  return status;
  }

  public BigDecimal getTotal() {
  return total;
  }

  public List<OrderLine> getLines() {
  return lines;
  }
  }

Thought · 3.4s
src\main\java\com\example\iaspringlab\exception\ResourceNotFoundException.java
package com.example.iaspringlab.exception;

/**
* Thrown when a requested resource does not exist. Mapped to HTTP 404.
  */
  public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
  super(message);
  }
  }

src\main\java\com\example\iaspringlab\exception\BusinessRuleException.java
package com.example.iaspringlab.exception;

/**
* Thrown when an operation violates a documented business rule. Mapped to HTTP 422.
  */
  public class BusinessRuleException extends RuntimeException {

  public BusinessRuleException(String message) {
  super(message);
  }
  }

src\main\java\com\example\iaspringlab\exception\DuplicateResourceException.java
package com.example.iaspringlab.exception;

/**
* Thrown when a resource would violate a uniqueness constraint. Mapped to HTTP 409.
  */
  public class DuplicateResourceException extends RuntimeException {

  public DuplicateResourceException(String message) {
  super(message);
  }
  }

src\main\java\com\example\iaspringlab\repository\ClientRepository.java
package com.example.iaspringlab.repository;

import com.example.iaspringlab.domain.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByEmail(String email);

    Optional<Client> findByEmail(String email);
}

src\main\java\com\example\iaspringlab\repository\ProductRepository.java
package com.example.iaspringlab.repository;

import com.example.iaspringlab.domain.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByReference(String reference);

    Optional<Product> findByReference(String reference);
}

src\main\java\com\example\iaspringlab\repository\OrderRepository.java
package com.example.iaspringlab.repository;

import com.example.iaspringlab.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}

Thought · 11.5s
src\main\java\com\example\iaspringlab\dto\CreateClientRequest.java
package com.example.iaspringlab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateClientRequest(
@NotBlank(message = "Le prénom est obligatoire") String firstName,
@NotBlank(message = "Le nom est obligatoire") String lastName,
@NotBlank(message = "L'email est obligatoire") @Email(message = "L'email est invalide") String email) {
}

src\main\java\com\example\iaspringlab\dto\ClientResponse.java
package com.example.iaspringlab.dto;

public record ClientResponse(
Long id,
String firstName,
String lastName,
String email) {
}

src\main\java\com\example\iaspringlab\dto\CreateProductRequest.java
package com.example.iaspringlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateProductRequest(
@NotBlank(message = "La référence est obligatoire") String reference,
@NotBlank(message = "Le nom est obligatoire") String name,
@NotNull(message = "Le prix est obligatoire")
@Positive(message = "Le prix doit être strictement positif") BigDecimal price) {
}

src\main\java\com\example\iaspringlab\dto\UpdateProductStatusRequest.java
package com.example.iaspringlab.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest(
@NotNull(message = "Le statut actif est obligatoire") Boolean active) {
}

src\main\java\com\example\iaspringlab\dto\ProductResponse.java
package com.example.iaspringlab.dto;

import java.math.BigDecimal;

public record ProductResponse(
Long id,
String reference,
String name,
BigDecimal price,
boolean active) {
}

src\main\java\com\example\iaspringlab\dto\OrderLineRequest.java
package com.example.iaspringlab.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineRequest(
@NotNull(message = "L'identifiant du produit est obligatoire") Long productId,
@Positive(message = "La quantité doit être strictement positive") int quantity) {
}

src\main\java\com\example\iaspringlab\dto\AddOrderLineRequest.java
package com.example.iaspringlab.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddOrderLineRequest(
@NotNull(message = "L'identifiant du produit est obligatoire") Long productId,
@Positive(message = "La quantité doit être strictement positive") int quantity) {
}

src\main\java\com\example\iaspringlab\dto\CreateOrderRequest.java
package com.example.iaspringlab.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
@NotNull(message = "L'identifiant du client est obligatoire") Long clientId,
@NotEmpty(message = "Une commande doit contenir au moins une ligne")
List<@Valid OrderLineRequest> lines) {
}

src\main\java\com\example\iaspringlab\dto\OrderLineResponse.java
package com.example.iaspringlab.dto;

import java.math.BigDecimal;

public record OrderLineResponse(
Long id,
Long productId,
String productReference,
int quantity,
BigDecimal unitPrice,
BigDecimal lineTotal) {
}

src\main\java\com\example\iaspringlab\dto\OrderResponse.java
package com.example.iaspringlab.dto;

import com.example.iaspringlab.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
Long id,
Long clientId,
String clientName,
OrderStatus status,
List<OrderLineResponse> lines,
BigDecimal total) {
}

src\main\java\com\example\iaspringlab\dto\DtoMapper.java
package com.example.iaspringlab.dto;

import com.example.iaspringlab.domain.Client;
import com.example.iaspringlab.domain.Order;
import com.example.iaspringlab.domain.OrderLine;
import com.example.iaspringlab.domain.Product;

/**
* Converts domain entities to their API representations. Entities are never exposed directly.
  */
  public final class DtoMapper {

  private DtoMapper() {
  }

  public static ClientResponse toClientResponse(Client client) {
  return new ClientResponse(
  client.getId(),
  client.getFirstName(),
  client.getLastName(),
  client.getEmail());
  }

  public static ProductResponse toProductResponse(Product product) {
  return new ProductResponse(
  product.getId(),
  product.getReference(),
  product.getName(),
  product.getPrice(),
  product.isActive());
  }

  public static OrderResponse toOrderResponse(Order order) {
  return new OrderResponse(
  order.getId(),
  order.getClient().getId(),
  order.getClient().getFirstName() + " " + order.getClient().getLastName(),
  order.getStatus(),
  order.getLines().stream().map(DtoMapper::toOrderLineResponse).toList(),
  order.getTotal());
  }

  private static OrderLineResponse toOrderLineResponse(OrderLine line) {
  return new OrderLineResponse(
  line.getId(),
  line.getProduct().getId(),
  line.getProduct().getReference(),
  line.getQuantity(),
  line.getUnitPrice(),
  line.getLineTotal());
  }
  }

Thought · 12.2s
src\main\java\com\example\iaspringlab\service\ClientService.java
package com.example.iaspringlab.service;

import com.example.iaspringlab.domain.Client;
import com.example.iaspringlab.dto.ClientResponse;
import com.example.iaspringlab.dto.CreateClientRequest;
import com.example.iaspringlab.dto.DtoMapper;
import com.example.iaspringlab.exception.DuplicateResourceException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.repository.ClientRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public ClientResponse create(CreateClientRequest request) {
        if (clientRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Un client existe déjà avec l'email : " + request.email());
        }
        Client client = new Client(request.firstName(), request.lastName(), request.email());
        return DtoMapper.toClientResponse(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public ClientResponse get(Long id) {
        return DtoMapper.toClientResponse(load(id));
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return clientRepository.findAll().stream()
                .map(DtoMapper::toClientResponse)
                .toList();
    }

    private Client load(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + id));
    }
}

src\main\java\com\example\iaspringlab\service\ProductService.java
package com.example.iaspringlab.service;

import com.example.iaspringlab.domain.Product;
import com.example.iaspringlab.dto.CreateProductRequest;
import com.example.iaspringlab.dto.DtoMapper;
import com.example.iaspringlab.dto.ProductResponse;
import com.example.iaspringlab.dto.UpdateProductStatusRequest;
import com.example.iaspringlab.exception.DuplicateResourceException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (productRepository.existsByReference(request.reference())) {
            throw new DuplicateResourceException("Un produit existe déjà avec la référence : " + request.reference());
        }
        Product product = new Product(request.reference(), request.name(), request.price());
        return DtoMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStatus(Long id, UpdateProductStatusRequest request) {
        Product product = load(id);
        if (Boolean.TRUE.equals(request.active())) {
            product.activate();
        } else {
            product.deactivate();
        }
        return DtoMapper.toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return DtoMapper.toProductResponse(load(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(DtoMapper::toProductResponse)
                .toList();
    }

    private Product load(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + id));
    }
}

src\main\java\com\example\iaspringlab\service\OrderService.java
package com.example.iaspringlab.service;

import com.example.iaspringlab.domain.Client;
import com.example.iaspringlab.domain.Order;
import com.example.iaspringlab.domain.OrderStatus;
import com.example.iaspringlab.domain.Product;
import com.example.iaspringlab.dto.AddOrderLineRequest;
import com.example.iaspringlab.dto.CreateOrderRequest;
import com.example.iaspringlab.dto.DtoMapper;
import com.example.iaspringlab.dto.OrderLineRequest;
import com.example.iaspringlab.dto.OrderResponse;
import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.repository.ClientRepository;
import com.example.iaspringlab.repository.OrderRepository;
import com.example.iaspringlab.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        ClientRepository clientRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
    }

    /**
     * Creates a DRAFT order for a customer, with at least one line.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + request.clientId()));

        if (request.lines() == null || request.lines().isEmpty()) {
            throw new BusinessRuleException("Une commande doit contenir au moins une ligne");
        }

        Order order = new Order(client);
        for (OrderLineRequest line : request.lines()) {
            addLineInternal(order, line.productId(), line.quantity());
        }
        return DtoMapper.toOrderResponse(orderRepository.save(order));
    }

    /**
     * Adds a line to a DRAFT order. An inactive product cannot be added.
     */
    @Transactional
    public OrderResponse addLine(Long orderId, AddOrderLineRequest request) {
        Order order = loadOrder(orderId);
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Impossible d'ajouter une ligne à une commande au statut " + order.getStatus());
        }
        addLineInternal(order, request.productId(), request.quantity());
        return DtoMapper.toOrderResponse(order);
    }

    /**
     * Transition DRAFT -> CONFIRMED.
     */
    @Transactional
    public OrderResponse confirm(Long orderId) {
        Order order = loadOrder(orderId);
        order.transitionTo(OrderStatus.CONFIRMED);
        return DtoMapper.toOrderResponse(order);
    }

    /**
     * Transition DRAFT -> CANCELLED or CONFIRMED -> CANCELLED.
     */
    @Transactional
    public OrderResponse cancel(Long orderId) {
        Order order = loadOrder(orderId);
        order.transitionTo(OrderStatus.CANCELLED);
        return DtoMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream()
                .map(DtoMapper::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return DtoMapper.toOrderResponse(loadOrder(id));
    }

    private void addLineInternal(Order order, Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + productId));
        if (!product.isActive()) {
            throw new BusinessRuleException(
                    "Le produit '" + product.getReference() + "' est inactif et ne peut pas être ajouté à une commande");
        }
        order.addLine(product, quantity);
    }

    private Order loadOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + id));
    }
}

Thought · 10.6s
src\main\java\com\example\iaspringlab\web\ApiError.java
package com.example.iaspringlab.web;

import java.time.Instant;
import java.util.List;

/**
* Standard error payload returned by {@link ApiExceptionHandler}.
  */
  public record ApiError(
  Instant timestamp,
  int status,
  String error,
  String message,
  String path,
  List<String> details) {
  }

src\main\java\com\example\iaspringlab\web\ApiExceptionHandler.java
package com.example.iaspringlab.web;

import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.exception.DuplicateResourceException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
* Translates application exceptions into consistent HTTP error responses.
  */
  @RestControllerAdvice
  public class ApiExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
  return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
  return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
  return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
  List<String> details = ex.getBindingResult().getFieldErrors().stream()
  .map(error -> error.getField() + " : " + error.getDefaultMessage())
  .toList();
  return build(HttpStatus.BAD_REQUEST, "Requête invalide", request, details);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
  return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne", request, List.of());
  }

  private ResponseEntity<ApiError> build(HttpStatus status, String message,
  HttpServletRequest request, List<String> details) {
  ApiError error = new ApiError(
  Instant.now(),
  status.value(),
  status.getReasonPhrase(),
  message,
  request.getRequestURI(),
  details);
  return ResponseEntity.status(status).body(error);
  }
  }

src\main\java\com\example\iaspringlab\web\ClientController.java
package com.example.iaspringlab.web;

import com.example.iaspringlab.dto.ClientResponse;
import com.example.iaspringlab.dto.CreateClientRequest;
import com.example.iaspringlab.service.ClientService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@Valid @RequestBody CreateClientRequest request) {
        return clientService.create(request);
    }

    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable Long id) {
        return clientService.get(id);
    }

    @GetMapping
    public List<ClientResponse> findAll() {
        return clientService.findAll();
    }
}

src\main\java\com\example\iaspringlab\web\ProductController.java
package com.example.iaspringlab.web;

import com.example.iaspringlab.dto.CreateProductRequest;
import com.example.iaspringlab.dto.ProductResponse;
import com.example.iaspringlab.dto.UpdateProductStatusRequest;
import com.example.iaspringlab.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PatchMapping("/{id}/status")
    public ProductResponse updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody UpdateProductStatusRequest request) {
        return productService.updateStatus(id, request);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.get(id);
    }

    @GetMapping
    public List<ProductResponse> findAll() {
        return productService.findAll();
    }
}

src\main\java\com\example\iaspringlab\web\OrderController.java
package com.example.iaspringlab.web;

import com.example.iaspringlab.dto.AddOrderLineRequest;
import com.example.iaspringlab.dto.CreateOrderRequest;
import com.example.iaspringlab.dto.OrderResponse;
import com.example.iaspringlab.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @PostMapping("/{id}/lines")
    public OrderResponse addLine(@PathVariable Long id,
                                 @Valid @RequestBody AddOrderLineRequest request) {
        return orderService.addLine(id, request);
    }

    @PostMapping("/{id}/confirm")
    public OrderResponse confirm(@PathVariable Long id) {
        return orderService.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.get(id);
    }

    @GetMapping
    public List<OrderResponse> findAll() {
        return orderService.findAll();
    }
}

Thought · 5.4s
src\main\resources\application.properties
spring.application.name=ia-spring-lab

# PostgreSQL datasource (override with environment variables in other environments)
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/iaspringlab}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:iaspringlab}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:iaspringlab}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate: the schema is owned by Flyway, Hibernate only validates it
spring.jpa.open-in-view=false
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=validate

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Actuator
management.endpoints.web.exposure.include=health,info

src\main\resources\db\migration\V1__init_schema.sql
-- Initial schema: order management.
-- Business rules: docs/business-rules.md

CREATE TABLE client (
id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
first_name  VARCHAR(255) NOT NULL,
last_name   VARCHAR(255) NOT NULL,
email       VARCHAR(255) NOT NULL,
CONSTRAINT uk_client_email UNIQUE (email)
);

CREATE TABLE product (
id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
reference  VARCHAR(255) NOT NULL,
name       VARCHAR(255) NOT NULL,
price      NUMERIC(19, 2) NOT NULL,
active     BOOLEAN NOT NULL DEFAULT TRUE,
CONSTRAINT uk_product_reference UNIQUE (reference)
);

CREATE TABLE orders (
id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
client_id  BIGINT NOT NULL,
status     VARCHAR(255) NOT NULL,
total      NUMERIC(19, 2) NOT NULL,
CONSTRAINT fk_orders_client FOREIGN KEY (client_id) REFERENCES client (id)
);

CREATE TABLE order_line (
id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
order_id    BIGINT NOT NULL,
product_id  BIGINT NOT NULL,
quantity    INTEGER NOT NULL,
unit_price  NUMERIC(19, 2) NOT NULL,
CONSTRAINT fk_order_line_order FOREIGN KEY (order_id) REFERENCES orders (id),
CONSTRAINT fk_order_line_product FOREIGN KEY (product_id) REFERENCES product (id),
CONSTRAINT ck_order_line_quantity_positive CHECK (quantity > 0)
);

docker-compose.yml
services:
postgres:
image: postgres:16-alpine
container_name: iaspringlab-postgres
environment:
POSTGRES_DB: iaspringlab
POSTGRES_USER: iaspringlab
POSTGRES_PASSWORD: iaspringlab
ports:
- "5432:5432"
volumes:
- iaspringlab-pgdata:/var/lib/postgresql/data
healthcheck:
test: ["CMD-SHELL", "pg_isready -U iaspringlab -d iaspringlab"]
interval: 5s
timeout: 5s
retries: 10

volumes:
iaspringlab-pgdata:

Thought · 792ms
Main code is in place. Let me compile it before writing tests.
$ .\mvnw.cmd -B -ntp -DskipTests compile 2>&1 | Select-Object -Last 40
(20 earlier lines)
[INFO] /D:/Obsidian_Vault/formation IA/ia-spring-lab/src/main/java/com/example/iaspringlab/web/ApiExceptionHandler.java: Recompile with -Xlint:deprecation for details.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.387 s
[INFO] Finished at: 2026-10-13T10:02:55+02:00
[INFO] ------------------------------------------------------------------------
Thought · 2.9s
Compiles. Let me identify the deprecation warning.
$ $sw = Get-ChildItem "$env:USERPROFILE\.m2\repository\org\springframework\spring-web\7.0.9\spring-web-7.0.9.jar"; & javap -cp $sw.FullName org.springframework.http.HttpStatus | Select-String 'UNPROCESSABLE|getReasonPhrase'
public static final org.springframework.http.HttpStatus UNPROCESSABLE_CONTENT;
public static final org.springframework.http.HttpStatus UNPROCESSABLE_ENTITY;
public java.lang.String getReasonPhrase();
Thought · 1.3s
src\main\java\com\example\iaspringlab\web\ApiExceptionHandler.java
}    }

    @ExceptionHandler(BusinessRuleException.class)    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, List.of());        return build(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request, List.of());
    }    }

    @ExceptionHandler(MethodArgumentNotValidException.class)    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
Thought · 37.4s
pom.xml
<artifactId>spring-boot-starter-test</artifactId>            <artifactId>spring-boot-starter-test</artifactId>
<scope>test</scope>            <scope>test</scope>
</dependency>        </dependency>
<dependency>        <dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-testcontainers</artifactId>
<scope>test</scope>
</dependency>
<dependency>
<groupId>org.testcontainers</groupId>            <groupId>org.testcontainers</groupId>
<artifactId>testcontainers</artifactId>            <artifactId>testcontainers</artifactId>
<scope>test</scope>            <scope>test</scope>
</dependency>        </dependency>
$ .\mvnw.cmd -B -ntp -DskipTests dependency:go-offline 2>&1 | Select-String 'BUILD|spring-boot-testcontainers' | Select-Object -Last 5
$jar = Get-ChildItem "$env:USERPROFILE\.m2\repository\org\springframework\boot\spring-boot-testcontainers\4.1.1\spring-boot-testcontaine…
[INFO]       plexus-build-api-0.0.7.jar
[INFO]       spring-boot-buildpack-platform-4.1.1.jar
[INFO]       plexus-build-api-0.0.7.jar
[INFO] Resolved dependency: spring-boot-testcontainers-4.1.1.jar
[INFO] BUILD SUCCESS
=== ServiceConnection location ===
org/springframework/boot/testcontainers/service/connection/ServiceConnection.class
Thought · 4.4s
$ $jar = Get-ChildItem "$env:USERPROFILE\.m2\repository\org\springframework\boot\spring-boot-testcontainers\4.1.1\spring-boot-testcontainers-4.1.1.jar"; & jar tf $jar.FullName | Select-String 'Postgre|Jdbc|JdbcConnectionDetails'…
=== SpringBoot services registration ===
Thought · 1.3s
$ $jar = Get-ChildItem "$env:USERPROFILE\.m2\repository\org\springframework\boot\spring-boot-testcontainers\4.1.1\spring-boot-testcontainers-4.1.1.jar"; & jar tf $jar.FullName | Select-Object -First 60
(54 earlier lines)
org/springframework/boot/testcontainers/service/connection/ServiceConnectionAutoConfigurationRegistrar.class
org/springframework/boot/testcontainers/service/connection/ServiceConnectionContextCustomizer$CacheKey.class
org/springframework/boot/testcontainers/service/connection/ServiceConnectionContextCustomizer.class
org/springframework/boot/testcontainers/service/connection/ServiceConnectionContextCustomizerFactory.class
org/springframework/boot/testcontainers/service/connection/Ssl.class
org/springframework/boot/testcontainers/service/connection/SslBundleSource.class
Thought · 7.4s
$ $repo="$env:USERPROFILE\.m2\repository"; Get-ChildItem $repo -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'spring-boot' } | ForEach-Object { $entries = & jar tf $_.FullName 2>$null | Select-String 'ConnectionDetailsFactory' ; if …
(73 earlier lines)
spring-boot-micrometer-metrics-4.1.1.jar :: org/springframework/boot/micrometer/metrics/testcontainers/otlp/OpenTelemet
ryMetricsContainerConnectionDetailsFactory.class
spring-boot-testcontainers-4.1.1-sources.jar ::
org/springframework/boot/testcontainers/service/connection/ContainerConnectionDetailsFactory.java
spring-boot-testcontainers-4.1.1.jar :: org/springframework/boot/testcontainers/service/connection/ContainerConnectionD
etailsFactory$ContainerConnectionDetails.class
Thought · 36.5s
Now the tests. First, domain unit tests.
src\test\java\com\example\iaspringlab\domain\OrderStatusTest.java
package com.example.iaspringlab.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void draftCanMoveToConfirmedOrCancelled() {
        assertThat(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void draftCannotStayDraft() {
        assertThat(OrderStatus.DRAFT.canTransitionTo(OrderStatus.DRAFT)).isFalse();
    }

    @Test
    void confirmedCanOnlyBeCancelled() {
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.DRAFT)).isFalse();
    }

    @Test
    void cancelledIsTerminal() {
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.DRAFT)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void cannotTransitionToNull() {
        assertThat(OrderStatus.DRAFT.canTransitionTo(null)).isFalse();
    }
}

src\test\java\com\example\iaspringlab\domain\OrderTest.java
package com.example.iaspringlab.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.iaspringlab.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Client CLIENT = new Client("Ada", "Lovelace", "ada@example.com");

    private static Product product(String reference, String price) {
        return new Product(reference, "Produit " + reference, new BigDecimal(price));
    }

    @Test
    void newOrderIsDraftWithZeroTotalAndNoLine() {
        Order order = new Order(CLIENT);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getLines()).isEmpty();
    }

    @Test
    void addLineComputesTotal() {
        Order order = new Order(CLIENT);

        order.addLine(product("A", "12.50"), 3);

        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getTotal()).isEqualByComparingTo("37.50");
    }

    @Test
    void addLineSnapshotsProductUnitPrice() {
        Product product = product("A", "12.50");
        Order order = new Order(CLIENT);

        order.addLine(product, 3);

        OrderLine line = order.getLines().getFirst();
        assertThat(line.getUnitPrice()).isEqualByComparingTo("12.50");
        assertThat(line.getLineTotal()).isEqualByComparingTo("37.50");
    }

    @Test
    void totalIsTheSumOfAllLines() {
        Order order = new Order(CLIENT);

        order.addLine(product("A", "10.00"), 2);
        order.addLine(product("B", "5.50"), 4);

        assertThat(order.getTotal()).isEqualByComparingTo("42.00");
    }

    @Test
    void addLineRejectsNonPositiveQuantity() {
        Order order = new Order(CLIENT);

        assertThatThrownBy(() -> order.addLine(product("A", "10.00"), 0))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void draftCanBeConfirmed() {
        Order order = new Order(CLIENT);

        order.transitionTo(OrderStatus.CONFIRMED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void illegalTransitionIsRejected() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CONFIRMED))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}

Thought · 11.4s
src\test\java\com\example\iaspringlab\service\ClientServiceTest.java
package com.example.iaspringlab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.iaspringlab.domain.Client;
import com.example.iaspringlab.dto.CreateClientRequest;
import com.example.iaspringlab.exception.DuplicateResourceException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.repository.ClientRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    void createRejectsDuplicateEmail() {
        when(clientRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.create(new CreateClientRequest("Ada", "Lovelace", "ada@example.com")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createSavesClient() {
        when(clientRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = clientService.create(new CreateClientRequest("Ada", "Lovelace", "ada@example.com"));

        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.lastName()).isEqualTo("Lovelace");
        assertThat(response.email()).isEqualTo("ada@example.com");
    }

    @Test
    void getThrowsWhenClientMissing() {
        when(clientRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.get(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

src\test\java\com\example\iaspringlab\service\ProductServiceTest.java
package com.example.iaspringlab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.iaspringlab.domain.Product;
import com.example.iaspringlab.dto.CreateProductRequest;
import com.example.iaspringlab.dto.UpdateProductStatusRequest;
import com.example.iaspringlab.exception.DuplicateResourceException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createRejectsDuplicateReference() {
        when(productRepository.existsByReference("REF-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(new CreateProductRequest("REF-1", "Produit", new BigDecimal("10.00"))))
                .isInstanceOf(DuplicateResourceException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void createSavesActiveProduct() {
        when(productRepository.existsByReference("REF-1")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = productService.create(new CreateProductRequest("REF-1", "Produit", new BigDecimal("10.00")));

        assertThat(response.reference()).isEqualTo("REF-1");
        assertThat(response.active()).isTrue();
    }

    @Test
    void updateStatusDeactivatesProduct() {
        Product product = new Product("REF-1", "Produit", new BigDecimal("10.00"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        var response = productService.updateStatus(1L, new UpdateProductStatusRequest(false));

        assertThat(response.active()).isFalse();
    }

    @Test
    void updateStatusActivatesProduct() {
        Product product = new Product("REF-1", "Produit", new BigDecimal("10.00"));
        product.deactivate();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        var response = productService.updateStatus(1L, new UpdateProductStatusRequest(true));

        assertThat(response.active()).isTrue();
    }

    @Test
    void getThrowsWhenProductMissing() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.get(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

src\test\java\com\example\iaspringlab\service\OrderServiceTest.java
package com.example.iaspringlab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.iaspringlab.domain.Client;
import com.example.iaspringlab.domain.Order;
import com.example.iaspringlab.domain.OrderStatus;
import com.example.iaspringlab.domain.Product;
import com.example.iaspringlab.dto.AddOrderLineRequest;
import com.example.iaspringlab.dto.CreateOrderRequest;
import com.example.iaspringlab.dto.OrderLineRequest;
import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.repository.ClientRepository;
import com.example.iaspringlab.repository.OrderRepository;
import com.example.iaspringlab.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private static final Client CLIENT = new Client("Ada", "Lovelace", "ada@example.com");

    private static Product product(String price) {
        return new Product("REF-1", "Produit", new BigDecimal(price));
    }

    @Test
    void createOrderRejectsUnknownClient() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new CreateOrderRequest(99L, List.of(new OrderLineRequest(1L, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrderRejectsEmptyLines() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(CLIENT));

        var request = new CreateOrderRequest(1L, List.of());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createOrderRejectsInactiveProduct() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(CLIENT));
        Product inactive = product("10.00");
        inactive.deactivate();
        when(productRepository.findById(1L)).thenReturn(Optional.of(inactive));

        var request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(1L, 2)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessRuleException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderComputesTotalFromLines() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(CLIENT));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product("10.00")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.createOrder(
                new CreateOrderRequest(1L, List.of(new OrderLineRequest(1L, 3))));

        assertThat(response.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(response.total()).isEqualByComparingTo("30.00");
    }

    @Test
    void addLineIsRejectedWhenOrderIsNotDraft() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CONFIRMED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.addLine(5L, new AddOrderLineRequest(1L, 1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addLineAddsLineAndRecomputesTotal() {
        Order order = new Order(CLIENT);
        order.addLine(product("10.00"), 1);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product("20.00")));

        var response = orderService.addLine(5L, new AddOrderLineRequest(2L, 2));

        assertThat(response.lines()).hasSize(2);
        assertThat(response.total()).isEqualByComparingTo("50.00");
    }

    @Test
    void confirmMovesDraftToConfirmed() {
        Order order = new Order(CLIENT);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        var response = orderService.confirm(3L);

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelMovesConfirmedToCancelled() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CONFIRMED);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        var response = orderService.cancel(3L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void confirmIsRejectedOnCancelledOrder() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CANCELLED);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirm(3L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getThrowsWhenOrderMissing() {
        when(orderRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.get(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

Thought · 6.2s
src\test\java\com\example\iaspringlab\web\OrderControllerTest.java
package com.example.iaspringlab.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.iaspringlab.domain.OrderStatus;
import com.example.iaspringlab.dto.OrderResponse;
import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class OrderControllerTest {

    private OrderService orderService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createReturns201WithTheOrder() throws Exception {
        when(orderService.createOrder(any())).thenReturn(
                new OrderResponse(1L, 1L, "Ada Lovelace", OrderStatus.DRAFT, List.of(), new BigDecimal("20.00")));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[{\"productId\":1,\"quantity\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.total").value(20.00));
    }

    @Test
    void createRejectsEmptyLinesWith400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void businessRuleViolationReturns422() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new BusinessRuleException("Règle métier violée"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Règle métier violée"));
    }
}

src\test\java\com\example\iaspringlab\web\ClientControllerTest.java
package com.example.iaspringlab.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.iaspringlab.dto.ClientResponse;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class ClientControllerTest {

    private ClientService clientService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        clientService = mock(ClientService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ClientController(clientService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createReturns201WithTheClient() throws Exception {
        when(clientService.create(any())).thenReturn(new ClientResponse(1L, "Ada", "Lovelace", "ada@example.com"));

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"email\":\"ada@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void createRejectsInvalidEmailWith400() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingClientReturns404() throws Exception {
        when(clientService.get(42L)).thenThrow(new ResourceNotFoundException("Client introuvable : 42"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/clients/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Client introuvable : 42"));
    }
}

Thought · 7.4s
src\test\java\com\example\iaspringlab\AbstractIntegrationTest.java
package com.example.iaspringlab;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
* Base class for integration tests backed by a real PostgreSQL instance started with
* Testcontainers. The whole class is skipped (not failed) when Docker is not available,
* so the suite can still run on developer machines without Docker.
  */
  @SpringBootTest
  @EnabledIfDockerAvailable
  public abstract class AbstractIntegrationTest {

  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  static {
  POSTGRES.start();
  }

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
  registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
  registry.add("spring.datasource.username", POSTGRES::getUsername);
  registry.add("spring.datasource.password", POSTGRES::getPassword);
  registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
  }
  }

src\test\java\com\example\iaspringlab\IaSpringLabApplicationTests.java
package com.example.iaspringlab;

import org.junit.jupiter.api.Test;

/**
* Verifies that the Spring context (Flyway migration included) starts against PostgreSQL.
  */
  class IaSpringLabApplicationTests extends AbstractIntegrationTest {

  @Test
  void contextLoads() {
  // The context starts only if Flyway migrated the schema and Hibernate validated it.
  }
  }

src\test\java\com\example\iaspringlab\service\OrderServiceIntegrationTest.java
package com.example.iaspringlab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.iaspringlab.AbstractIntegrationTest;
import com.example.iaspringlab.domain.OrderStatus;
import com.example.iaspringlab.dto.AddOrderLineRequest;
import com.example.iaspringlab.dto.ClientResponse;
import com.example.iaspringlab.dto.CreateClientRequest;
import com.example.iaspringlab.dto.CreateOrderRequest;
import com.example.iaspringlab.dto.CreateProductRequest;
import com.example.iaspringlab.dto.OrderLineRequest;
import com.example.iaspringlab.dto.OrderResponse;
import com.example.iaspringlab.dto.ProductResponse;
import com.example.iaspringlab.dto.UpdateProductStatusRequest;
import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.exception.DuplicateResourceException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProductService productService;

    private ClientResponse newClient() {
        String email = "client-" + UUID.randomUUID() + "@example.com";
        return clientService.create(new CreateClientRequest("Ada", "Lovelace", email));
    }

    private ProductResponse newProduct(String price) {
        String reference = "REF-" + UUID.randomUUID();
        return productService.create(new CreateProductRequest(reference, "Produit", new BigDecimal(price)));
    }

    @Test
    void fullOrderLifecycle() {
        ClientResponse client = newClient();
        ProductResponse product = newProduct("12.50");

        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest(client.id(), List.of(new OrderLineRequest(product.id(), 3))));

        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.total()).isEqualByComparingTo("37.50");
        assertThat(order.lines()).singleElement()
                .satisfies(line -> {
                    assertThat(line.quantity()).isEqualTo(3);
                    assertThat(line.unitPrice()).isEqualByComparingTo("12.50");
                    assertThat(line.lineTotal()).isEqualByComparingTo("37.50");
                });

        OrderResponse withExtraLine = orderService.addLine(order.id(), new AddOrderLineRequest(product.id(), 1));
        assertThat(withExtraLine.total()).isEqualByComparingTo("50.00");

        OrderResponse confirmed = orderService.confirm(order.id());
        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);

        assertThatThrownBy(() -> orderService.addLine(order.id(), new AddOrderLineRequest(product.id(), 1)))
                .isInstanceOf(BusinessRuleException.class);

        OrderResponse cancelled = orderService.cancel(order.id());
        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);

        assertThat(orderService.get(order.id()).total()).isEqualByComparingTo("50.00");
    }

    @Test
    void inactiveProductCannotBeAddedToAnOrder() {
        ClientResponse client = newClient();
        ProductResponse product = newProduct("9.99");
        productService.updateStatus(product.id(), new UpdateProductStatusRequest(false));

        assertThatThrownBy(() -> orderService.createOrder(
                new CreateOrderRequest(client.id(), List.of(new OrderLineRequest(product.id(), 1)))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void duplicateClientEmailIsRejected() {
        ClientResponse client = newClient();

        assertThatThrownBy(() -> clientService.create(
                new CreateClientRequest("Grace", "Hopper", client.email())))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void unitPriceIsTheSnapshotTakenWhenTheLineIsAdded() {
        ClientResponse client = newClient();
        ProductResponse product = newProduct("100.00");

        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest(client.id(), List.of(new OrderLineRequest(product.id(), 2))));

        assertThat(order.lines()).singleElement()
                .satisfies(line -> assertThat(line.unitPrice()).isEqualByComparingTo("100.00"));
        assertThat(order.total()).isEqualByComparingTo("200.00");
    }
}

Thought · 3.6s
All source and tests are written. Let me run the full suite.
$ .\mvnw.cmd -B -ntp test 2>&1 | Select-Object -Last 70
(46 earlier lines)
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

Exited with code 1
Thought · 1.9s
Testcontainers 2.x made PostgreSQLContainer non-generic. Let me fix that.
src\test\java\com\example\iaspringlab\AbstractIntegrationTest.java
@SpringBootTest@SpringBootTest
@EnabledIfDockerAvailable@EnabledIfDockerAvailable
public abstract class AbstractIntegrationTest {public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {    static {
        POSTGRES.start();        POSTGRES.start();
    }    }
$ .\mvnw.cmd -B -ntp test 2>&1 | Select-Object -Last 80
(75 earlier lines)
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

Exited with code 1
Thought · 15.0s
All 33 unit/web tests pass. The Testcontainers skip condition can't live on the abstract base. Let me move it to the concrete classes.
src\test\java\com\example\iaspringlab\AbstractIntegrationTest.java

import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
/**
* Base class for integration tests backed by a real PostgreSQL instance started with/**
* Testcontainers. The whole class is skipped (not failed) when Docker is not available, * Base class for integration tests backed by a real PostgreSQL instance started with
* so the suite can still run on developer machines without Docker. * Testcontainers. Concrete test classes must be annotated with

*/ * {@link org.testcontainers.junit.jupiter.EnabledIfDockerAvailable} so that the suite is
@SpringBootTest * skipped (not failed) when Docker is not available.
@EnabledIfDockerAvailable */
public abstract class AbstractIntegrationTest {@SpringBootTest

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

src\test\java\com\example\iaspringlab\IaSpringLabApplicationTests.java
package com.example.iaspringlab;package com.example.iaspringlab;

import org.junit.jupiter.api.Test;import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

/**
/** * Verifies that the Spring context (Flyway migration included) starts against PostgreSQL.
* Verifies that the Spring context (Flyway migration included) starts against PostgreSQL. * Skipped when Docker is not available.
  */
  */@EnabledIfDockerAvailable
  class IaSpringLabApplicationTests extends AbstractIntegrationTest {
  class IaSpringLabApplicationTests extends AbstractIntegrationTest {
  @Test
  @Test    void contextLoads() {
  void contextLoads() {
  src\test\java\com\example\iaspringlab\service\OrderServiceIntegrationTest.java
  import java.util.List;import java.util.List;
  import java.util.UUID;import java.util.UUID;
  import org.junit.jupiter.api.Test;import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;import org.springframework.beans.factory.annotation.Autowired;
  import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

@EnabledIfDockerAvailable
class OrderServiceIntegrationTest extends AbstractIntegrationTest {
class OrderServiceIntegrationTest extends AbstractIntegrationTest {
@Autowired
@Autowired    private OrderService orderService;
private OrderService orderService;
$ .\mvnw.cmd -B -ntp test 2>&1 | Select-Object -Last 45
(38 earlier lines)
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.777 s
[INFO] Finished at: 2026-10-13T10:07:14+02:00
[INFO] ------------------------------------------------------------------------
Error: Provider request failed with HTTP 400: {"model":"big-pickle"}
Build · Big Pickle · 6m 58s
reprend
Thought · 16.6s
Suite de la tâche : tout le code compile et les tests passent (41 tests, 5 intégration ignorés faute de Docker). Il reste la documentation (README.md, docs/api.md) et la revue par les agents. Je continue.
README.md
# AI Spring Lab

REST API de gestion de commandes (clients, produits, commandes) écrite avec Java 21 et Spring Boot 4.

## Stack

- Java 21, Maven (wrapper `mvnw`)
- Spring Boot 4.1.1 : Spring Web MVC, Spring Data JPA, Bean Validation, Actuator, Flyway
- PostgreSQL 16 (driver `org.postgresql`)
- Tests : JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers
- Couverture de schéma par migrations Flyway (`ddl-auto=validate`)

## Prérequis

- JDK 21
- Docker (pour le `docker-compose.yml` et pour les tests d'intégration Testcontainers)
- Aucun Maven local requis : le wrapper télécharge Maven 3.10.0

## Lancer l'application

```bash
# 1. Démarrer PostgreSQL
docker compose up -d

# 2. Lancer l'API (http://localhost:8080)
./mvnw spring-boot:run          # Windows : .\mvnw.cmd spring-boot:run
```

La configuration par défaut se connecte à `jdbc:postgresql://localhost:5432/iaspringlab`
(base/utilisateur/mot de passe `iaspringlab`). Surchargez-la avec les variables
d'environnement `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`.

Flyway applique `src/main/resources/db/migration/V1__init_schema.sql` au démarrage.

## Lancer les tests

```bash
./mvnw test                                        # suite complète
./mvnw test -Dtest=OrderServiceTest                # une classe
./mvnw test -Dtest=OrderServiceTest#addLineAddsLineAndRecomputesTotal   # une méthode
```

- Les tests unitaires et web (Mockito / MockMvc) tournent sans base de données.
- Les tests d'intégration (`*IntegrationTest`) démarrent un PostgreSQL éphémère via
  **Testcontainers**. Ils sont **automatiquement ignorés** si Docker est absent
  (annotation `@EnabledIfDockerAvailable`), afin que la suite reste verte sans Docker.

## API

Base : `/api`. Documentation détaillée : [`docs/api.md`](docs/api.md).

| Méthode | Chemin | Rôle |
|--------|--------|------|
| `POST` | `/api/clients` | Créer un client |
| `GET` | `/api/clients` · `/api/clients/{id}` | Lister / consulter |
| `POST` | `/api/products` | Créer un produit |
| `PATCH` | `/api/products/{id}/status` | Activer / désactiver |
| `GET` | `/api/products` · `/api/products/{id}` | Lister / consulter |
| `POST` | `/api/orders` | Créer une commande (DRAFT) |
| `GET` | `/api/orders` · `/api/orders/{id}` | Lister / consulter |
| `POST` | `/api/orders/{id}/lines` | Ajouter une ligne (commande DRAFT) |
| `POST` | `/api/orders/{id}/confirm` | DRAFT → CONFIRMED |
| `POST` | `/api/orders/{id}/cancel` | → CANCELLED |

Erreurs normalisées (`ApiError`) : `400` validation, `404` introuvable, `409` doublon
(email client ou référence produit), `422` règle métier violée.

## Règles métier

Source de vérité : [`docs/business-rules.md`](docs/business-rules.md).

- Client : prénom, nom, email unique.
- Produit : référence, nom, prix, statut actif/inactif. Un produit inactif ne peut pas
  être ajouté à une commande.
- Commande : appartient à un seul client, au moins une ligne, quantité strictement
  positive par ligne, prix unitaire figé au moment de l'ajout de la ligne.
- Total = somme(quantité × prix unitaire).
- Transitions de statut autorisées : `DRAFT → CONFIRMED`, `DRAFT → CANCELLED`,
  `CONFIRMED → CANCELLED`. Aucune autre.

## Architecture

Flux `Controller → Service → Repository` (les contrôleurs n'accèdent jamais aux
repositories). Les entités JPA ne sont jamais exposées : les échanges passent par des
DTO `record`. Les transactions sont déclarées aux frontières des services.

```
src/main/java/com/example/iaspringlab
├── domain/       Entités JPA + OrderStatus (transitions)
├── dto/          Requêtes/réponses (records) + DtoMapper
├── exception/    Exceptions applicatives
├── repository/   Spring Data JPA
├── service/      Logique métier et transactions
└── web/          Contrôleurs REST + gestion d'erreurs
```

## Limites connues

- Un produit inactif est refusé aussi bien à la création d'une commande qu'à l'ajout
  d'une ligne (interprétation de la règle « ne peut pas être ajouté à une nouvelle
  commande »).
- L'ajout d'une ligne n'est autorisé que sur une commande au statut `DRAFT`.
- La modification du prix d'un produit n'est pas exposée par l'API (aucune règle métier
  ne la définit).

docs\api.md
# API Reference

Base URL: `/api`. All request and response bodies are JSON.

Errors use a single shape:

```json
{
  "timestamp": "2026-10-13T08:00:00Z",
  "status": 422,
  "error": "Unprocessable Content",
  "message": "Une commande doit contenir au moins une ligne",
  "path": "/api/orders",
  "details": []
}
```

| Status | Meaning |
|--------|---------|
| 400 | Body validation failed (`details` lists the field errors) |
| 404 | Resource not found |
| 409 | Uniqueness conflict (client email, product reference) |
| 422 | Business rule violated |

## Clients

### `POST /api/clients` → 201

```json
{ "firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com" }
```

```json
{ "id": 1, "firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com" }
```

`409` if the email already exists.

### `GET /api/clients` · `GET /api/clients/{id}`

Returns a client or a list of clients. `404` if the id is unknown.

## Products

### `POST /api/products` → 201

```json
{ "reference": "REF-1", "name": "Keyboard", "price": 49.90 }
```

```json
{ "id": 1, "reference": "REF-1", "name": "Keyboard", "price": 49.90, "active": true }
```

`409` if the reference already exists.

### `PATCH /api/products/{id}/status`

```json
{ "active": false }
```

Activates or deactivates the product.

### `GET /api/products` · `GET /api/products/{id}`

Returns a product or a list of products.

## Orders

### `POST /api/orders` → 201

Creates a `DRAFT` order with at least one line.

```json
{
  "clientId": 1,
  "lines": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ]
}
```

```json
{
  "id": 10,
  "clientId": 1,
  "clientName": "Ada Lovelace",
  "status": "DRAFT",
  "lines": [
    { "id": 100, "productId": 1, "productReference": "REF-1", "quantity": 2, "unitPrice": 49.90, "lineTotal": 99.80 }
  ],
  "total": 99.80
}
```

- `404` if the client or a product is unknown.
- `422` if the order has no line, a quantity is not strictly positive, or a product is
  inactive.

### `POST /api/orders/{id}/lines`

Adds a line to a `DRAFT` order and recomputes the total.

```json
{ "productId": 3, "quantity": 5 }
```

`422` if the order is not `DRAFT`, if the product is inactive, or if the quantity is not
strictly positive.

### `POST /api/orders/{id}/confirm`

`DRAFT → CONFIRMED`. `422` for any other source status.

### `POST /api/orders/{id}/cancel`

`DRAFT → CANCELLED` or `CONFIRMED → CANCELLED`. `422` if the order is already
`CANCELLED`.

### `GET /api/orders` · `GET /api/orders/{id}`

Returns an order or a list of orders.

Thought · 7.6s
Now I'll run the mandated review/test subagents on the implementation.
Reviewer Subagent — Review Spring Boot implementation
Tester Subagent — Review test strategy
Thought · 33.5s
Both agents delivered substantive findings. I'll fix the high-value, verifiable defects they identified: the catch-all 500 handler (F1), null elements in lines (F2), duplicate-race 409 (F4), the null line id on addLine (F5), plus documentation/test gaps. Let me start with the exception handler.
src\main\java\com\example\iaspringlab\web\ApiExceptionHandler.java
package com.example.iaspringlab.web;

import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.exception.DuplicateResourceException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
* Translates application and Spring MVC exceptions into consistent HTTP error responses.
*
* <p>Standard protocol errors (malformed body, bad path variable, unsupported method or
* media type, unknown path) are mapped to 4xx; only genuinely unexpected errors produce a 500.</p>
  */
  @RestControllerAdvice
  public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
  return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
  return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
  return build(HttpStatus.CONFLICT, "Conflit d'unicité sur la ressource", request, List.of());
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
  return build(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
  List<String> details = ex.getBindingResult().getFieldErrors().stream()
  .map(error -> error.getField() + " : " + error.getDefaultMessage())
  .toList();
  return build(HttpStatus.BAD_REQUEST, "Requête invalide", request, details);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
  return build(HttpStatus.BAD_REQUEST, "Corps de requête illisible ou invalide", request, List.of());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
  return build(HttpStatus.BAD_REQUEST,
  "Paramètre invalide : " + ex.getName() + "=" + ex.getValue(), request, List.of());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
  HttpServletRequest request) {
  return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiError> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
  HttpServletRequest request) {
  return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public ResponseEntity<ApiError> handleNoResource(Exception ex, HttpServletRequest request) {
  return build(HttpStatus.NOT_FOUND, "Ressource introuvable : " + request.getRequestURI(), request, List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
  log.error("Erreur inattendue sur {}", request.getRequestURI(), ex);
  return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne", request, List.of());
  }

  private ResponseEntity<ApiError> build(HttpStatus status, String message,
  HttpServletRequest request, List<String> details) {
  ApiError error = new ApiError(
  Instant.now(),
  status.value(),
  status.getReasonPhrase(),
  message,
  request.getRequestURI(),
  details);
  return ResponseEntity.status(status).body(error);
  }
  }

Thought · 3.3s
src\main\java\com\example\iaspringlab\dto\CreateOrderRequest.java

public record CreateOrderRequest(public record CreateOrderRequest(
@NotNull(message = "L'identifiant du client est obligatoire") Long clientId,        @NotNull(message = "L'identifiant du client est obligatoire") Long clientId,
@NotEmpty(message = "Une commande doit contenir au moins une ligne")        @NotEmpty(message = "Une commande doit contenir au moins une ligne")
List<@Valid OrderLineRequest> lines) {        List<@NotNull(message = "Une ligne de commande est obligatoire") @Valid OrderLineRequest> lines) {
}
}
src\main\java\com\example\iaspringlab\dto\CreateProductRequest.java
public record CreateProductRequest(public record CreateProductRequest(
@NotBlank(message = "La référence est obligatoire") String reference,        @NotBlank(message = "La référence est obligatoire") String reference,
@NotBlank(message = "Le nom est obligatoire") String name,        @NotBlank(message = "Le nom est obligatoire") String name,
@NotNull(message = "Le prix est obligatoire")        @NotNull(message = "Le prix est obligatoire")
@Positive(message = "Le prix doit être strictement positif") BigDecimal price) {        @Positive(message = "Le prix doit être strictement positif")
@Digits(integer = 17, fraction = 2, message = "Le prix doit avoir au plus 2 décimales")
BigDecimal price) {
}
}
src\main\java\com\example\iaspringlab\dto\CreateProductRequest.java
package com.example.iaspringlab.dto;package com.example.iaspringlab.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;import java.math.BigDecimal;
Thought · 2.2s
src\main\java\com\example\iaspringlab\domain\Order.java
@Column(name = "total", nullable = false, precision = 19, scale = 2)    @Column(name = "total", nullable = false, precision = 19, scale = 2)
private BigDecimal total = BigDecimal.ZERO;    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<OrderLine> lines = new ArrayList<>();    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {    protected Order() {
        // required by JPA        // required by JPA
src\main\java\com\example\iaspringlab\domain\Order.java
import jakarta.persistence.Id;import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;import jakarta.persistence.Table;
import java.math.BigDecimal;import java.math.BigDecimal;
import java.util.ArrayList;import java.util.ArrayList;
import java.util.List;import java.util.List;
src\main\java\com\example\iaspringlab\domain\Order.java
return total;        return total;
}    }

    public List<OrderLine> getLines() {    public List<OrderLine> getLines() {
        return lines;        return List.copyOf(lines);
    }    }
}}
Thought · 2.4s
src\main\java\com\example\iaspringlab\service\OrderService.java
throw new BusinessRuleException(            throw new BusinessRuleException(
"Impossible d'ajouter une ligne à une commande au statut " + order.getStatus());                    "Impossible d'ajouter une ligne à une commande au statut " + order.getStatus());
}        }
addLineInternal(order, request.productId(), request.quantity());        addLineInternal(order, request.productId(), request.quantity());
return DtoMapper.toOrderResponse(order);        return DtoMapper.toOrderResponse(orderRepository.saveAndFlush(order));
}
}
/**
/**     * Transition DRAFT -> CONFIRMED.
* Transition DRAFT -> CONFIRMED.
src\main\java\com\example\iaspringlab\repository\ClientRepository.java

public interface ClientRepository extends JpaRepository<Client, Long> {public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByEmail(String email);    boolean existsByEmail(String email);

    Optional<Client> findByEmail(String email);
}}
src\main\java\com\example\iaspringlab\repository\ProductRepository.java

public interface ProductRepository extends JpaRepository<Product, Long> {public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByReference(String reference);    boolean existsByReference(String reference);

    Optional<Product> findByReference(String reference);
}}
Thought · 1.2s
src\main\java\com\example\iaspringlab\repository\ClientRepository.java
package com.example.iaspringlab.repository;package com.example.iaspringlab.repository;

import com.example.iaspringlab.domain.Client;import com.example.iaspringlab.domain.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {public interface ClientRepository extends JpaRepository<Client, Long> {

src\main\java\com\example\iaspringlab\repository\ProductRepository.java
package com.example.iaspringlab.repository;package com.example.iaspringlab.repository;

import com.example.iaspringlab.domain.Product;import com.example.iaspringlab.domain.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {public interface ProductRepository extends JpaRepository<Product, Long> {

Thought · 1.6s
src\main\resources\db\migration\V1__init_schema.sql
reference  VARCHAR(255) NOT NULL,    reference  VARCHAR(255) NOT NULL,
name       VARCHAR(255) NOT NULL,    name       VARCHAR(255) NOT NULL,
price      NUMERIC(19, 2) NOT NULL,    price      NUMERIC(19, 2) NOT NULL,
active     BOOLEAN NOT NULL DEFAULT TRUE,    active     BOOLEAN NOT NULL DEFAULT TRUE,
CONSTRAINT uk_product_reference UNIQUE (reference)    CONSTRAINT uk_product_reference UNIQUE (reference),
CONSTRAINT ck_product_price_positive CHECK (price > 0)
););

CREATE TABLE orders (CREATE TABLE orders (
id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
src\main\resources\db\migration\V1__init_schema.sql
CONSTRAINT fk_order_line_order FOREIGN KEY (order_id) REFERENCES orders (id),    CONSTRAINT fk_order_line_order FOREIGN KEY (order_id) REFERENCES orders (id),
CONSTRAINT fk_order_line_product FOREIGN KEY (product_id) REFERENCES product (id),    CONSTRAINT fk_order_line_product FOREIGN KEY (product_id) REFERENCES product (id),
CONSTRAINT ck_order_line_quantity_positive CHECK (quantity > 0)    CONSTRAINT ck_order_line_quantity_positive CHECK (quantity > 0)
););

-- PostgreSQL does not index foreign keys automatically
CREATE INDEX ix_orders_client_id ON orders (client_id);
CREATE INDEX ix_order_line_order_id ON order_line (order_id);
CREATE INDEX ix_order_line_product_id ON order_line (product_id);

Thought · 2.0s
docs\api.md
"total": 99.80  "total": 99.80
}}
``````

- `400` if the body fails validation (no line, or a quantity is not strictly positive).
- `404` if the client or a product is unknown.
- `404` if the client or a product is unknown.- `422` if a product is inactive.
- `422` if the order has no line, a quantity is not strictly positive, or a product is
  inactive.

### `POST /api/orders/{id}/lines`### `POST /api/orders/{id}/lines`

Adds a line to a `DRAFT` order and recomputes the total.Adds a line to a `DRAFT` order and recomputes the total.
docs\api.md
```json```json
{ "productId": 3, "quantity": 5 }{ "productId": 3, "quantity": 5 }
``````

`422` if the order is not `DRAFT`, if the product is inactive, or if the quantity is not`400` if the quantity is not strictly positive; `422` if the order is not `DRAFT` or if
strictly positive.the product is inactive.

### `POST /api/orders/{id}/confirm`### `POST /api/orders/{id}/confirm`

`DRAFT → CONFIRMED`. `422` for any other source status.`DRAFT → CONFIRMED`. `422` for any other source status.
Thought · 5.6s
Now let me strengthen the tests per the tester agent's findings: a missing ProductControllerTest, ordered-controller gaps, and more service/domain edge cases.
src\test\java\com\example\iaspringlab\web\ProductControllerTest.java
package com.example.iaspringlab.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.iaspringlab.dto.ProductResponse;
import com.example.iaspringlab.exception.DuplicateResourceException;
import com.example.iaspringlab.service.ProductService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class ProductControllerTest {

    private ProductService productService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createReturns201WithActiveProduct() throws Exception {
        when(productService.create(any())).thenReturn(
                new ProductResponse(1L, "REF-1", "Keyboard", new BigDecimal("49.90"), true));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"REF-1\",\"name\":\"Keyboard\",\"price\":49.90}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("REF-1"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createRejectsNonPositivePriceWith400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"REF-1\",\"name\":\"Keyboard\",\"price\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Requête invalide"));
    }

    @Test
    void duplicateReferenceReturns409() throws Exception {
        when(productService.create(any())).thenThrow(new DuplicateResourceException("Un produit existe déjà"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"REF-1\",\"name\":\"Keyboard\",\"price\":9.99}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Un produit existe déjà"));
    }

    @Test
    void patchStatusDeactivatesProduct() throws Exception {
        when(productService.updateStatus(eq(1L), any())).thenReturn(
                new ProductResponse(1L, "REF-1", "Keyboard", new BigDecimal("9.99"), false));

        mockMvc.perform(patch("/api/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void patchStatusRejectsMissingActiveWith400() throws Exception {
        mockMvc.perform(patch("/api/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}

Thought · 8.4s
src\test\java\com\example\iaspringlab\web\OrderControllerTest.java
package com.example.iaspringlab.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.iaspringlab.domain.OrderStatus;
import com.example.iaspringlab.dto.OrderLineResponse;
import com.example.iaspringlab.dto.OrderResponse;
import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class OrderControllerTest {

    private OrderService orderService;
    private MockMvc mockMvc;

    private static OrderResponse draftOrder() {
        return new OrderResponse(1L, 1L, "Ada Lovelace", OrderStatus.DRAFT,
                List.of(new OrderLineResponse(100L, 1L, "REF-1", 2, new BigDecimal("10.00"), new BigDecimal("20.00"))),
                new BigDecimal("20.00"));
    }

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createReturns201WithTheOrder() throws Exception {
        when(orderService.createOrder(any())).thenReturn(draftOrder());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[{\"productId\":1,\"quantity\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(20.00))
                .andExpect(jsonPath("$.total").value(20.00));
    }

    @Test
    void createRejectsEmptyLinesWith400AndDetail() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Requête invalide"))
                .andExpect(jsonPath("$.details[0]", containsString("lines")));
    }

    @Test
    void createRejectsNonPositiveQuantityWith400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[{\"productId\":1,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("quantity")));
    }

    @Test
    void createRejectsNullLineWith400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[null]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void businessRuleViolationReturns422() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new BusinessRuleException("Règle métier violée"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"lines\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Règle métier violée"));
    }

    @Test
    void addLineReturns200WithRecomputedTotal() throws Exception {
        when(orderService.addLine(eq(1L), any())).thenReturn(draftOrder());

        mockMvc.perform(post("/api/orders/1/lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":2,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(20.00));
    }

    @Test
    void confirmReturns200WithConfirmedStatus() throws Exception {
        when(orderService.confirm(1L)).thenReturn(new OrderResponse(1L, 1L, "Ada Lovelace",
                OrderStatus.CONFIRMED, List.of(), new BigDecimal("20.00")));

        mockMvc.perform(post("/api/orders/1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        when(orderService.get(42L)).thenThrow(new ResourceNotFoundException("Commande introuvable : 42"));

        mockMvc.perform(get("/api/orders/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Commande introuvable : 42"));
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(put("/api/orders/1"))
                .andExpect(status().isMethodNotAllowed());
    }
}

Thought · 7.2s
src\test\java\com\example\iaspringlab\service\OrderServiceTest.java

        assertThatThrownBy(() -> orderService.get(42L))        assertThatThrownBy(() -> orderService.get(42L))
                .isInstanceOf(ResourceNotFoundException.class);                .isInstanceOf(ResourceNotFoundException.class);
    }    }

    @Test
    void createOrderRejectsUnknownProduct() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(CLIENT));
        when(productRepository.findById(7L)).thenReturn(Optional.empty());

        var request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(7L, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderRejectsNonPositiveQuantity() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(CLIENT));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product("10.00")));

        var request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(1L, 0)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("strictement positive");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderSumsAllLines() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(CLIENT));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product("10.00")));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product("2.50")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateOrderRequest(1L,
                List.of(new OrderLineRequest(1L, 3), new OrderLineRequest(2L, 4)));

        var response = orderService.createOrder(request);

        assertThat(response.lines()).hasSize(2);
        assertThat(response.total()).isEqualByComparingTo("40.00");
    }

    @Test
    void addLineRejectsUnknownOrder() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.addLine(99L, new AddOrderLineRequest(1L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addLineRejectsInactiveProduct() {
        Order order = new Order(CLIENT);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        Product inactive = product("10.00");
        inactive.deactivate();
        when(productRepository.findById(2L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> orderService.addLine(5L, new AddOrderLineRequest(2L, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactif");
        assertThat(order.getLines()).isEmpty();
    }

    @Test
    void addLineIsRejectedWhenOrderIsCancelled() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CANCELLED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.addLine(5L, new AddOrderLineRequest(1L, 1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void confirmIsRejectedWhenOrderIsAlreadyConfirmed() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CONFIRMED);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirm(3L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancelMovesDraftToCancelled() {
        Order order = new Order(CLIENT);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        var response = orderService.cancel(3L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelIsRejectedWhenOrderIsAlreadyCancelled() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CANCELLED);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(3L))
                .isInstanceOf(BusinessRuleException.class);
    }
}
























}
Thought · 4.4s
src\test\java\com\example\iaspringlab\domain\OrderTest.java
assertThatThrownBy(() -> order.transitionTo(OrderStatus.CONFIRMED))        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CONFIRMED))
.isInstanceOf(BusinessRuleException.class);                .isInstanceOf(BusinessRuleException.class);
assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
}    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void addLineRejectsNonPositiveQuantityWithoutMutatingOrder(int quantity) {
        Order order = new Order(CLIENT);

        assertThatThrownBy(() -> order.addLine(product("A", "10.00"), quantity))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("strictement positive");
        assertThat(order.getLines()).isEmpty();
        assertThat(order.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void draftCanBeCancelledDirectly() {
        Order order = new Order(CLIENT);

        order.transitionTo(OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void confirmedCanBeCancelled() {
        Order order = new Order(CLIENT);
        order.transitionTo(OrderStatus.CONFIRMED);

        order.transitionTo(OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void transitionToSameStatusIsRejected() {
        Order order = new Order(CLIENT);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.DRAFT))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void transitionToNullIsRejected() {
        Order order = new Order(CLIENT);

        assertThatThrownBy(() -> order.transitionTo(null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void totalUsesExactDecimalArithmetic() {
        Order order = new Order(CLIENT);

        order.addLine(product("A", "0.10"), 3);
        order.addLine(product("B", "19.99"), 3);

        assertThat(order.getTotal()).isEqualByComparingTo("60.27");
    }
}






}
src\test\java\com\example\iaspringlab\domain\OrderTest.java

import com.example.iaspringlab.exception.BusinessRuleException;import com.example.iaspringlab.exception.BusinessRuleException;
import java.math.BigDecimal;import java.math.BigDecimal;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderTest {class OrderTest {

    private static final Client CLIENT = new Client("Ada", "Lovelace", "ada@example.com");    private static final Client CLIENT = new Client("Ada", "Lovelace", "ada@example.com");
Thought · 3.4s
src\test\java\com\example\iaspringlab\service\OrderServiceIntegrationTest.java
.isInstanceOf(DuplicateResourceException.class);                .isInstanceOf(DuplicateResourceException.class);
}    }

    @Test    @Test
    void unitPriceIsTheSnapshotTakenWhenTheLineIsAdded() {    void unitPriceIsCapturedFromTheProductWhenTheLineIsCreated() {
        ClientResponse client = newClient();
        ClientResponse client = newClient();        ProductResponse product = newProduct("100.00");
        ProductResponse product = newProduct("100.00");
        OrderResponse order = orderService.createOrder(
        OrderResponse order = orderService.createOrder(
@@ -108,5 +108,37 @@
assertThat(order.lines()).singleElement()        assertThat(order.lines()).singleElement()
.satisfies(line -> assertThat(line.unitPrice()).isEqualByComparingTo("100.00"));                .satisfies(line -> assertThat(line.unitPrice()).isEqualByComparingTo("100.00"));
assertThat(order.total()).isEqualByComparingTo("200.00");        assertThat(order.total()).isEqualByComparingTo("200.00");
}    }

    @Test
    void confirmingTwiceIsRejected() {
        ClientResponse client = newClient();
        ProductResponse product = newProduct("10.00");
        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest(client.id(), List.of(new OrderLineRequest(product.id(), 1))));
        orderService.confirm(order.id());

        assertThatThrownBy(() -> orderService.confirm(order.id()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void draftOrderCanBeCancelledDirectly() {
        ClientResponse client = newClient();
        ProductResponse product = newProduct("10.00");
        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest(client.id(), List.of(new OrderLineRequest(product.id(), 1))));

        assertThat(orderService.cancel(order.id()).status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void responseExposesClientFullName() {
        ClientResponse client = newClient();
        ProductResponse product = newProduct("10.00");
        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest(client.id(), List.of(new OrderLineRequest(product.id(), 1))));

        assertThat(order.clientName()).isEqualTo("Ada Lovelace");
    }
}





}
Thought · 4.0s
Let me run the full suite again to validate all fixes.
$ .\mvnw.cmd -B -ntp test 2>&1 | Select-Object -Last 55
(50 earlier lines)
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

Exited with code 1
Thought · 2.5s
One stub needs updating since addLine now flushes.
```java
src\test\java\com\example\iaspringlab\service\OrderServiceTest.java
    void addLineAddsLineAndRecomputesTotal() {                                                                          void addLineAddsLineAndRecomputesTotal() {
        Order order = new Order(CLIENT);                                                                                    Order order = new Order(CLIENT);
        order.addLine(product("10.00"), 1);                                                                                 order.addLine(product("10.00"), 1);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));                                                  when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
                                                                                                                            when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product("20.00")));                                     when(productRepository.findById(2L)).thenReturn(Optional.of(product("20.00")));
        var response = orderService.addLine(5L, new AddOrderLineRequest(2L, 2));                                            var response = orderService.addLine(5L, new AddOrderLineRequest(2L, 2));
```
````
$ .\mvnw.cmd -B -ntp test 2>&1 | Select-Object -Last 18
10:30:42.984 [main] INFO org.springframework.mock.web.MockServletContext -- Initializing Spring TestDispatcherServlet ''
10:30:42.984 [main] INFO org.springframework.test.web.servlet.TestDispatcherServlet -- Initializing Servlet ''
10:30:42.984 [main] INFO org.springframework.test.web.servlet.TestDispatcherServlet -- Completed initialization in 0 ms
10:30:43.003 [main] INFO org.springframework.mock.web.MockServletContext -- Initializing Spring TestDispatcherServlet ''
10:30:43.003 [main] INFO org.springframework.test.web.servlet.TestDispatcherServlet -- Initializing Servlet ''
10:30:43.003 [main] INFO org.springframework.test.web.servlet.TestDispatcherServlet -- Completed initialization in 0 ms
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.166 s -- in com.example.iaspringlab.web.ProductControllerTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 73, Failures: 0, Errors: 0, Skipped: 8
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  11.422 s
[INFO] Finished at: 2026-10-13T10:30:43+02:00
[INFO] ------------------------------------------------------------------------
````
Compaction· 106.3K in · 1.8K out
Objective
Apply the instructions in prompte2.md: build the complete "AI Spring Lab" order-management REST API in this repository (Java 21 + Spring Boot), generating real files, running verification, and ending with a report.
Requirements
Follow AGENTS.md conventions and use docs/business-rules.md as the sole source of business rules; do not invent rules; ask for clarification on blocking ambiguity.
Stack required by prompte2.md: Maven, Spring Web, Spring Data JPA, Bean Validation, PostgreSQL, JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers.
Strict Controller → Service → Repository flow; controllers must not access repositories directly.
Produce/adapt: pom.xml, Java classes (controllers, services, repositories, entities, DTOs), exception handling, validation, unit + integration tests, PostgreSQL/Testcontainers config, Spring config, docker-compose.yml if needed, README.md, .gitignore.
Use custom agents @tester, @reviewer, @explainer where relevant; do not claim to have invoked an agent unless actually invoked.
Final verification: compile, run unit tests, run integration tests when the environment permits, fix errors, inspect git diff, check conformance, then report files created, features, commands run, real test results, and limitations.
Decisions
Switched datasource from the pre-existing H2 setup to PostgreSQL per prompte2.md; kept Flyway (schema ownership) and ddl-auto=validate.
Spring Boot 4.1.1 modular starters: spring-boot-starter-webmvc, spring-boot-starter-validation, spring-boot-starter-data-jpa, spring-boot-starter-actuator, spring-boot-starter-flyway, plus flyway-database-postgresql, org.postgresql:postgresql (runtime).
Docker is NOT installed, so integration tests are skipped gracefully via @EnabledIfDockerAvailable on concrete test classes (not the abstract base, which caused an error), and @DynamicPropertySource wires the container.
Interpretations documented as assumptions: inactive-product check applies to any line addition (create and add-line); adding a line is restricted to DRAFT orders.
Testcontainers 2.0.5 specifics: use org.testcontainers.postgresql.PostgreSQLContainer (non-generic, no <?>); test deps testcontainers, testcontainers-postgresql, testcontainers-junit-jupiter, plus spring-boot-testcontainers.
Web tests use standalone MockMvc with explicit LocalValidatorFactoryBean (no Spring context); service/domain tests use Mockito.
Work State
Completed
Main source tree written: domain (Client, Product, Order, OrderLine, OrderStatus), DTOs (records + DtoMapper), repositories, services (ClientService, ProductService, OrderService), web (ClientController, ProductController, OrderController, ApiError, ApiExceptionHandler), exceptions (ResourceNotFoundException, BusinessRuleException, DuplicateResourceException).
Config/resources: pom.xml rewritten, src/main/resources/application.properties (PostgreSQL + Flyway), src/main/resources/db/migration/V1__init_schema.sql, docker-compose.yml.
Tests written: OrderStatusTest, OrderTest, ClientServiceTest, ProductServiceTest, OrderServiceTest, OrderControllerTest, ClientControllerTest, AbstractIntegrationTest, IaSpringLabApplicationTests (rewritten), OrderServiceIntegrationTest.
.\mvnw.cmd -B -ntp test now passes: Tests run: 41, Failures: 0, Errors: 0, Skipped: 5 (5 skipped = Testcontainers integration tests, Docker unavailable).
Fixed a deprecation by using HttpStatus.UNPROCESSABLE_CONTENT (instead of UNPROCESSABLE_ENTITY) in ApiExceptionHandler.
Active
Remaining prompte2 deliverables: update/create README.md (currently 0 bytes) and docs/api.md (currently 0 bytes); optionally .gitignore.
Still to do: invoke @tester/@reviewer/@explainer, inspect git diff, and produce the final report.
Blocked
Testcontainers integration tests cannot actually execute here (no Docker); they compile and skip.
Next Move
Write README.md (project, stack, docker compose up + .\mvnw.cmd spring-boot:run, how to run tests, note integration tests need Docker) and fill docs/api.md with the REST endpoints.
Optionally update .gitignore if needed (existing already ignores target/, HELP.md, .idea).
Invoke the reviewer and tester subagents on the changes; address findings.
Run .\mvnw.cmd -B -ntp test once more and inspect git diff.
Produce the final French report (files created, features, commands executed, real test results, limitations) as required by prompte2.md.
Relevant Files
prompte2.md: the mission prompt being applied (72 lines, French).
docs/business-rules.md: authoritative business rules (Customer/Product/Order/Total/Status).
AGENTS.md: coding conventions to respect.
pom.xml: rewritten build; Spring Boot 4.1.1, PostgreSQL, Testcontainers 2.0.5.
src/main/java/com/example/iaspringlab/service/OrderService.java: core business orchestration.
src/main/java/com/example/iaspringlab/domain/Order.java: totals, line handling, status transitions.
src/main/java/com/example/iaspringlab/web/ApiExceptionHandler.java: HTTP error mapping (404/409/422/400/500).
src/main/resources/db/migration/V1__init_schema.sql: PostgreSQL schema (tables client, product, orders, order_line).
src/main/resources/application.properties: PostgreSQL datasource + Flyway + ddl-auto=validate.
src/test/java/com/example/iaspringlab/AbstractIntegrationTest.java: Testcontainers base (static container start + @DynamicPropertySource).
src/test/java/com/example/iaspringlab/service/OrderServiceIntegrationTest.java: integration tests (Docker-gated).
README.md: empty, needs content.
docs/api.md: empty, needs API docs.
docker-compose.yml: PostgreSQL 16 service for local runs.
.opencode/agents/reviewer.md, .opencode/agents/tester.md, .opencode/agents/explainer.md: custom agent instructions to consult.
Important Context
mvnw is script-only (distributionType=only-script); downloads Maven 3.10.0. Run via .\mvnw.cmd on Windows PowerShell.
Spring Boot 4.1.1 details discovered: spring-boot-starter-test exists; starter names are split; Testcontainers 2.0.5 uses testcontainers-* prefixed artifacts; @EnabledIfDockerAvailable must be on concrete test classes (putting it on the abstract base threw ConditionEvaluationException: required test class is not present in the current ExtensionContext).
Earlier (pre-task) repo state: current branch is orchestration; AGENTS.md was replaced by the user with the conventions content formerly in prompte2.md. .idea/ shelf files and HELP.md exist but are gitignored.
The app cannot start without a running PostgreSQL; docker-compose.yml provides one.
Java is OpenJDK 21.0.12.1; locale is fr_FR. User-facing/validation messages in the code are French.
Changes are uncommitted in the working tree.
Thought · 1.5s
All 73 tests pass (8 skipped — the Docker-gated integration tests). Let me inspect the working tree and diff before finalizing.
$ git status --short; git diff --stat
(22 earlier lines)
warning: in the working copy of 'src/test/java/com/example/iaspringlab/IaSpringLabApplicationTests.java', LF will be replaced by CRLF the next time Git touches it
README.md                                          | 105 +++++++++++++++++
docs/api.md                                        | 124 +++++++++++++++++++++
pom.xml                                            |  38 ++++---
src/main/resources/application.properties          |  18 +++
.../iaspringlab/IaSpringLabApplicationTests.java   |  12 +-
5 files changed, 279 insertions(+), 18 deletions(-)
$ git status --porcelain --untracked-files=all | Select-String -NotMatch 'target/'
(42 earlier lines)
?? src/test/java/com/example/iaspringlab/service/ClientServiceTest.java
?? src/test/java/com/example/iaspringlab/service/OrderServiceIntegrationTest.java
?? src/test/java/com/example/iaspringlab/service/OrderServiceTest.java
?? src/test/java/com/example/iaspringlab/service/ProductServiceTest.java
?? src/test/java/com/example/iaspringlab/web/ClientControllerTest.java
?? src/test/java/com/example/iaspringlab/web/OrderControllerTest.java
?? src/test/java/com/example/iaspringlab/web/ProductControllerTest.java
Thought · 1.6s
There are stray META-INF/ files at the repo root. Let me inspect them before deciding.
$ Get-ChildItem -Recurse META-INF | Select-Object FullName, Length; Write-Output '---spring.factories---'; Get-Content META-INF/spring.factories; Write-Output '---imports---'; Get-Content META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
(14 earlier lines)
# Failure Analyzers
org.springframework.boot.diagnostics.FailureAnalyzer=\
org.springframework.boot.testcontainers.lifecycle.DockerEnvironmentNotFoundFailureAnalyzer
---imports---
org.springframework.boot.testcontainers.properties.TestcontainersPropertySourceAutoConfiguration
org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration
$ Get-Content .gitignore
(26 earlier lines)
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

### VS Code ###
.vscode/
Thought · 4.7s
$ Get-ChildItem -Recurse META-INF | Select-Object FullName, CreationTime, LastWriteTime | Format-List
(5 earlier lines)
CreationTime  : 13/10/2026 10:04:06
LastWriteTime : 01/02/1980 00:00:00

FullName      : D:\Obsidian_Vault\formation
IA\ia-spring-lab\META-INF\spring\org.springframework.boot.autoconfigure.AutoConfiguration.imports
CreationTime  : 13/10/2026 10:04:06
LastWriteTime : 01/02/1980 00:00:00
$ git log --oneline -5; git stash list
07a1e18 add business rules and explainer documentation orchestration des agent ia
750d716 add business rules and explainer documentation orchestration des agent ia
207246f add project documentation and clean up main application class
d0edeaa first
6c6d610 first
Thought · 7.9s
Those META-INF files have 1980 timestamps (content extracted from the spring-boot-testcontainers JAR) and were created during a test run at 10:04 — build noise, not source. Let me confirm by removing them and re-running.
$ Remove-Item -Recurse -Force META-INF; Write-Output "removed"; .\mvnw.cmd -B -ntp test 2>&1 | Select-String -Pattern 'Tests run:|BUILD' | Select-Object -Last 5
(4 earlier lines)
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.459 s -- in
com.example.iaspringlab.web.OrderControllerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.301 s -- in
com.example.iaspringlab.web.ProductControllerTest
[INFO] Tests run: 73, Failures: 0, Errors: 0, Skipped: 8
[INFO] BUILD SUCCESS
Thought · 456ms
$ if (Test-Path META-INF) { Write-Output "REAPPEARED"; Get-ChildItem -Recurse META-INF | Select-Object FullName } else { Write-Output "still absent" }; git status --short | Select-String 'META-INF'
still absent
Thought · 2.6s
No reappearance — the working tree is clean of build noise. Let me re-read the mission prompt to make sure I cover every required deliverable and report item.
Explored: 2 reads
Thought · 4.4s
I've used @reviewer and @tester; the prompt also asks for @explainer on architecture/technical choices. Let me invoke it, and in parallel run a final compile check.
Explainer Subagent — Explain architecture and choices
$ .\mvnw.cmd -B -ntp -o clean compile 2>&1 | Select-String -Pattern 'BUILD|ERROR|WARNING.*deprecat' | Select-Object -Last 8
[INFO] Building ia-spring-lab 0.0.1-SNAPSHOT
[INFO] BUILD SUCCESS
Thought · 2.2s
The @explainer mentioned a docs/REGLE_METIER.md — let me verify the docs directory and take a final look at the tree state.
$ Get-ChildItem docs | Select-Object Name, Length; Write-Output '--- final status ---'; git status --short
(16 earlier lines)
?? src/test/java/com/example/iaspringlab/web/
Name              Length
----              ------
api.md              2617
business-rules.md    635
REGLE_METIER.md      760
--- final status ---
$ Write-Output '--- main sources ---'; Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName.Replace((Get-Location).Path + '\','') }; Write-Output '--- tests ---'; Get-ChildItem -Recurse src/test/java -Filter *.java | ForEach-Object { $_.FullNa…
(45 earlier lines)
src\main\resources\db
src\main\resources\static
src\main\resources\templates
src\main\resources\application.properties
src\main\resources\db\migration
src\main\resources\db\migration\V1__init_schema.sql
Thought · 2.5s
Let me review the diff of the tracked files I modified, to confirm nothing unrelated changed.
$ git diff -- pom.xml src/main/resources/application.properties src/test/java/com/example/iaspringlab/IaSpringLabApplicationTests.java
(131 earlier lines)

     @Test
     void contextLoads() {
+        // The context starts only if Flyway migrated the schema and Hibernate validated it.
  }
-
}
Thought · 3.3s
$ Get-ChildItem target/surefire-reports -Filter '*.txt' | ForEach-Object { (Select-String -Path $_.FullName -Pattern '^Tests run:').Line -replace 'Tests run: ',' ' } ; Write-Output '--- names ---'; Get-ChildItem target/surefire-reports -Filter '*.txt' | ForEach-Object { $i…
(15 earlier lines)
+ CategoryInfo          : ObjectNotFound: (D:\Obsidian_Vau...urefire-reports:String) [Get-ChildItem], ItemNotFound
Exception
+ FullyQualifiedErrorId : PathNotFound,Microsoft.PowerShell.Commands.GetChildItemCommand


Exited with code 1
Thought · 1.1s
The clean wiped the reports. Let me do the final complete test run and capture per-class results.
$ .\mvnw.cmd -B -ntp test 2>&1 | Select-String -Pattern 'Tests run:.*in com|Tests run: \d+, Failures.*Skipped: \d+$|BUILD (SUCCESS|FAILURE)'
(16 earlier lines)
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.299 s -- in
com.example.iaspringlab.web.OrderControllerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.173 s -- in
com.example.iaspringlab.web.ProductControllerTest
[INFO] Tests run: 73, Failures: 0, Errors: 0, Skipped: 8
[INFO] BUILD SUCCESS
Thought · 13.5s
Tout est vert. Voici le compte rendu final.
Compte rendu — AI Spring Lab
1. Ce qui a été construit
   API REST de gestion de commandes (clients, produits, commandes) en Java 21 / Spring Boot 4.1.1, flux strict Controller → Service → Repository, avec PostgreSQL + Flyway et une pyramide de tests complète.
   Fichiers créés
   Configuration / infra
   docker-compose.yml — PostgreSQL 16-alpine + healthcheck pg_isready
   src/main/resources/application.properties — datasource PostgreSQL (surchargeable par env), ddl-auto=validate, open-in-view=false, Flyway, Actuator
   src/main/resources/db/migration/V1__init_schema.sql — schéma client, product, orders, order_line (unicités, FK, CHECK price>0, CHECK quantity>0, index sur les FK)
   README.md et docs/api.md (documentation complète des endpoints et du format d'erreur)
   src/main/java/com/example/iaspringlab
   domain/ — Client, Product, Order, OrderLine, OrderStatus (transitions autorisées portées par l'enum)
   dto/ — records d'entrée/sortie + DtoMapper
   repository/ — ClientRepository, ProductRepository, OrderRepository
   service/ — ClientService, ProductService, OrderService (logique métier + @Transactional)
   web/ — ClientController, ProductController, OrderController, ApiError, ApiExceptionHandler
   exception/ — ResourceNotFoundException, DuplicateResourceException, BusinessRuleException
   Tests src/test/java — OrderTest, OrderStatusTest, ClientServiceTest, ProductServiceTest, OrderServiceTest, ClientControllerTest, OrderControllerTest, ProductControllerTest, AbstractIntegrationTest, IaSpringLabApplicationTests, OrderServiceIntegrationTest.
   Fichiers modifiés
   pom.xml — passage H2 → PostgreSQL (spring-boot-starter-webmvc, data-jpa, validation, actuator, flyway, flyway-database-postgresql, org.postgresql), tests (spring-boot-starter-test, spring-boot-testcontainers, testcontainers, testcontainers-postgresql, testcontainers-junit-jupiter)
   IaSpringLabApplicationTests.java — vérifie le démarrage du contexte (Flyway + validation Hibernate), désormais basé Testcontainers
   .gitignore — revu et inchangé (couvre déjà target/, .idea, .vscode, etc.)
2. Fonctionnalités implémentées
   Clients : création (email unique → 409), consultation, liste.
   Produits : création (référence unique → 409), activation/désactivation (PATCH /status), consultation, liste.
   Commandes : création DRAFT avec au moins une ligne, ajout de ligne (uniquement si DRAFT), confirm (DRAFT→CONFIRMED), cancel (DRAFT→CANCELLED, CONFIRMED→CANCELLED), consultation, liste.
   Règles métier (issues de docs/business-rules.md) : total = Σ(quantité × prix unitaire) en BigDecimal/NUMERIC(19,2) ; prix unitaire figé à l'ajout de la ligne ; quantité strictement positive ; produit inactif refusé ; transitions de statut strictement limitées.
   Validation Bean Validation des corps de requête ; entités JPA jamais exposées (records + DtoMapper) ; erreurs normalisées ApiError avec mapping 400 / 404 / 405 / 409 / 415 / 422 / 500.
3. Agents personnalisés utilisés (réellement invoqués)
   @reviewer — a identifié des défauts réels, dont : JSON malformé/méthode non supportée/chemin inconnu renvoyant 500, lines:[null] provoquant une NPE, id de ligne nul en réponse d'addLine, course sur contrainte d'unicité → 500.
   @tester — a cartographié la couverture et proposé des scénarios manquants (aucun ProductControllerTest, gaps sur cancel/transitions/quantités).
   @explainer — a produit l'explication d'architecture et de choix techniques intégrée ci-dessous.
   Corrections appliquées suite aux revues : handler dédié pour les exceptions Spring MVC (400/404/405/415), List<@NotNull @Valid …> lines, gestion DataIntegrityViolationException → 409, saveAndFlush sur addLine (id de ligne présent), @Digits sur le prix, @OrderBy("id") sur les lignes, getLines() non modifiable, index FK + CHECK en base, suppression de méthodes de repository mortes, ajout de ProductControllerTest et de nombreux cas d'erreur/transition.
4. Commandes exécutées et résultats réels
   Commande	Résultat
   .\mvnw.cmd -B -ntp -o clean compile	BUILD SUCCESS
   .\mvnw.cmd -B -ntp test	BUILD SUCCESS — Tests run: 73, Failures: 0, Errors: 0, Skipped: 8
   git status / git diff	uniquement les fichiers liés au projet (aucun fichier sans rapport)
   Détail des tests (65 réellement exécutés) :
   Classe	Couche	Exécutés	Ignorés
   OrderStatusTest	domaine	5	0
   OrderTest	domaine	15	0
   ClientServiceTest	service (Mockito)	3	0
   ProductServiceTest	service (Mockito)	5	0
   OrderServiceTest	service (Mockito)	19	0
   ClientControllerTest	web (MockMvc standalone)	3	0
   OrderControllerTest	web (MockMvc standalone)	10	0
   ProductControllerTest	web (MockMvc standalone)	5	0
   IaSpringLabApplicationTests	intégration	—	1
   OrderServiceIntegrationTest	intégration	—	7
5. Limites et points à confirmer
   Tests d'intégration non exécutés ici : Docker n'est pas installé sur la machine. Les 8 tests Testcontainers sont compilés puis ignorés via @EnabledIfDockerAvailable. Ils tourneront avec docker compose up -d disponible (ou sur une CI dotée de Docker). C'est la seule partie de la stack non validée à l'exécution.
   Interprétation à confirmer (produit inactif) : la règle dit « inactive products cannot be added to new orders ». Le code refuse un produit inactif à la création d'une commande ET à l'ajout d'une ligne, mais ne revalide pas les lignes existantes lors du confirm. Merci de confirmer ce comportement.
   Ajout de ligne restreint au statut DRAFT : cohérent (on ne modifie pas une commande confirmée) mais absent de business-rules.md — à valider.
   Total dénormalisé (orders.total stocké) et absence de verrouillage optimiste (@Version) : suffisant pour l'exercice, à durcir en production (ajout concurrent de lignes = risque de lost update).
   Pas de garde-fou automatisé sur la règle de couches (pas d'ArchUnit) ; elle repose sur la convention respectée dans le code et documentée dans AGENTS.md.
   GET /api/orders n'est pas paginé et peut générer un N+1 (accès LAZY aux lignes/clients dans le mapping).