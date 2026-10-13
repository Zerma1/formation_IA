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
