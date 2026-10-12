# ia-spring-lab — Gestion de commandes

Application Spring Boot implémentant les règles métier décrites dans
[`REGLE_METIER.md`](REGLE_METIER.md) : clients, produits et commandes.

## Règles métier implémentées

### Client
- prénom, nom, email **unique**.

### Produit
- référence (unique), nom, prix, statut **actif / inactif**.
- un produit **inactif** ne peut pas être ajouté à une commande.

### Commande
- appartient à **exactement un client** ;
- contient **au moins une ligne** ;
- **quantité strictement positive** par ligne ;
- mémorise le **prix unitaire du produit au moment de la commande** (snapshot) ;
- total ligne = `quantité × prix unitaire` ;
- total commande = somme des lignes.

### Transitions de statut autorisées
```
DRAFT      -> CONFIRMED
DRAFT      -> CANCELLED
CONFIRMED  -> CANCELLED
```
Toute autre transition est refusée.

## Stack
- Java 17, Spring Boot 4.1.1
- Spring Web MVC, Spring Data JPA, Bean Validation, Spring Actuator
- Base H2 en mémoire, schéma géré par **Flyway** (`spring.jpa.hibernate.ddl-auto=validate`)
- Console H2 : `/h2-console` (JDBC URL `jdbc:h2:mem:orderdb`, user `sa`)

## Lancer l'application

```bash
./mvnw spring-boot:run
```

## Lancer les tests

```bash
./mvnw test
```

## API REST

### Clients
| Méthode | URL                | Description            |
|---------|--------------------|------------------------|
| POST    | `/api/clients`     | Créer un client        |
| GET     | `/api/clients`     | Lister les clients     |
| GET     | `/api/clients/{id}`| Récupérer un client    |

### Produits
| Méthode | URL                       | Description                       |
|---------|---------------------------|-----------------------------------|
| POST    | `/api/products`           | Créer un produit                  |
| GET     | `/api/products`           | Lister les produits               |
| GET     | `/api/products/{id}`      | Récupérer un produit              |
| PATCH   | `/api/products/{id}/status` | Activer / désactiver un produit |

### Commandes
| Méthode | URL                        | Description                          |
|---------|----------------------------|--------------------------------------|
| POST    | `/api/orders`              | Créer une commande (DRAFT)           |
| GET     | `/api/orders`              | Lister les commandes                 |
| GET     | `/api/orders/{id}`         | Récupérer une commande               |
| POST    | `/api/orders/{id}/lines`   | Ajouter une ligne (commande DRAFT)   |
| POST    | `/api/orders/{id}/confirm` | DRAFT -> CONFIRMED                   |
| POST    | `/api/orders/{id}/cancel`  | DRAFT/CONFIRMED -> CANCELLED         |

## Exemples

### Créer un client
```bash
curl -X POST http://localhost:8080/api/clients \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","email":"john@example.com"}'
```

### Créer un produit
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"reference":"REF-001","name":"Clavier","price":49.90}'
```

### Créer une commande
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"clientId":1,"lines":[{"productId":1,"quantity":2}]}'
```

### Confirmer une commande
```bash
curl -X POST http://localhost:8080/api/orders/1/confirm
```

## Structure du projet
```
src/main/java/com/example/iaspringlab/
├── domain/        # Entités JPA + OrderStatus (règles de transition)
├── repository/    # Repositories Spring Data JPA
├── service/       # Règles métier (transactions)
├── dto/           # Records requête/réponse + mapper
└── web/           # Contrôleurs REST + gestion d'erreurs
src/main/resources/db/migration/V1__init_schema.sql
```

## Codes d'erreur
| Code | Cas                                                     |
|------|---------------------------------------------------------|
| 400  | Requête invalide (validation Bean Validation)           |
| 404  | Ressource introuvable                                   |
| 409  | Violation de règle métier (produit inactif, transition…)|
