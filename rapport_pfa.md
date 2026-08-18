# Rapport de Projet de Fin d'Année (PFA)

## Collabix - Plateforme Collaborative Intelligente pour PME

---

## Dédicace

À mes chers parents, dont le soutien indéfectible et les sacrifices silencieux ont illuminé chacun de mes pas, je dédie ce travail avec tout mon amour et ma gratitude.

À mes formateurs, pour la sagesse transmise, la patience infinie et cette flamme du savoir que vous avez su allumer en moi, veuillez trouver ici l'expression de ma profonde reconnaissance.

À mon encadrant et à toute l'équipe d'Orchid Island Real Estate, pour leur confiance, leur accompagnement précieux et l'opportunité de donner vie à cette vision numérique.

Au travail d'équipe, à cette alchimie collective qui transforme les idées en réalisations, et à toutes celles et ceux qui ont contribué, de près ou de loin, à l'aboutissement de Collabix.

Puisse cette plateforme, née de la collaboration et éclairée par l'intelligence artificielle, être le reflet de ce que la passion, la rigueur et la solidarité peuvent bâtir ensemble.

---

## Remerciements

Avant toute chose, je tiens à exprimer ma profonde gratitude à mon encadrant pédagogique, dont les conseils éclairés, la rigueur et la disponibilité ont guidé chacune des étapes de ce projet. Sa bienveillance et son exigence ont été d'une valeur inestimable pour transformer une simple idée en une réalisation concrète et aboutie. Je remercie également mon directeur de stage, qui m'a accordé sa confiance, défini un cadre clair et permis de relever les défis de la transformation digitale d'une PME avec autonomie et sérénité.

Je souhaite adresser toute ma reconnaissance à l'équipe d'Orchid Island Real Estate, qui m'a chaleureusement accueilli et intégré dans son quotidien. À travers la gestion de multiples équipes, les enjeux de communication interne et l'automatisation des processus, ils m'ont offert un terrain d'expérimentation réel et précieux, faisant de ce stage une expérience aussi enrichissante humainement que professionnellement. Mes remerciements vont aussi à mes collègues, qui ont consacré du temps à tester la plateforme Collabix, à fournir des retours constructifs et à participer à son amélioration continue ; leur contribution a directement façonné la qualité du résultat final.

Enfin, je remercie du fond du cœur ma famille, qui m'a soutenu sans relâche tout au long de cette aventure. Leur patience, leurs encouragements et leur présence ont été le socle sur lequel j'ai pu avancer, persévérer et mener ce projet à son terme. Ce travail est aussi le fruit de leur dévouement et de leur confiance indéfectible.

---

## Résumé

Ce rapport présente le développement de Collabix, une plateforme collaborative intelligente de type SaaS conçue pour répondre aux besoins réels d'une PME du secteur immobilier. L'entreprise hôte, Orchid Island Real Estate, souffrait d'une communication interne fragmentée, d'une gestion des tâches peu centralisée et d'une absence d'automatisation intelligente, entraînant une perte d'informations et un ralentissement du travail collaboratif. Face à ce constat, le projet propose une plateforme unique centralisant les communications et les espaces de travail par département, la gestion des projets et des tâches, ainsi que la base de connaissances interne. Pour renforcer cette centralisation, la plateforme intègre plusieurs capacités d'IA : le module Handover AI automatise l'analyse des rapports de passation, la base de connaissances est structurée et interrogeable, et un assistant métier, Collabix AI, orchestre quatre modules — Analytics AI, Knowledge AI, Handover AI et Report AI — pour générer des tableaux de bord décisionnels et des rapports intelligents.

D'un point de vue technique, l'application repose sur une architecture moderne combinant React et TypeScript pour l'interface, NestJS et Node.js pour les services, PostgreSQL pour la persistance des données, ainsi que les modèles Gemini et Groq pour les capacités d'intelligence artificielle. Des permissions granulaires garantissent la sécurité des données et un contrôle d'accès précis pour chaque utilisateur. Les résultats obtenus démontrent une meilleure organisation du travail, une accélération des processus internes et une collaboration renforcée au sein de l'entreprise. Au-delà de la réponse immédiate au besoin exprimé, Collabix constitue une solution scalable, prête pour une exploitation SaaS, capable d'accompagner la transformation digitale d'autres PME.

**Mots-clés :** collaboration ; IA ; SaaS ; automatisation ; permissions granulaires ; centralisation.

---

## Introduction générale

Dans un environnement économique marqué par une concurrence accrue et une digitalisation accélérée, les petites et moyennes entreprises doivent repenser leurs modes de travail pour rester compétitives. La collaboration moderne ne se limite plus à de simples échanges d'informations : elle exige des outils centralisés, des processus fluides et une capacité à exploiter intelligemment les données produites au quotidien. Pourtant, force est de constater que de nombreuses PME s'appuient encore sur une multitude d'outils disparates — messageries, notes éparses, tableurs — qui fragmentent la communication et freinent la productivité.

Ce constat général trouve une illustration particulièrement parlante au sein d'Orchid Island Real Estate, agence immobilière de prestige basée à Marrakech. L'agence emploie plusieurs équipes fonctionnant en alternance, le matin et le soir, dont la coordination s'avère complexe. La communication se disperse sur plusieurs canaux : courriels, messages courts et notes manuscrites, rendant difficile le suivi des échanges. La gestion des tâches n'étant pas centralisée, il devient vite impossible de savoir qui fait quoi et à quel stade se trouve chaque dossier. Les passations entre équipes, faute d'être structurées, entraînent des pertes d'informations dommageables, tandis que la documentation se retrouve éparpillée entre différents supports. À cela s'ajoutent l'absence d'automatisation des tâches répétitives et le manque d'analyse intelligente des données internes.

Ces difficultés ne sont pas sans conséquences. Elles ralentissent la productivité des équipes, augmentent le risque d'erreurs, alourdissent la charge mentale des collaborateurs et, en définitive, dégradent la qualité de service offerte aux clients. Sur le plan des ressources humaines, la rotation des équipes et les départs fragilisent la continuité du travail lorsque le savoir n'est ni formalisé ni transmis efficacement.

Face à ce constat, le présent projet propose le développement de Collabix, une plateforme collaborative de type SaaS pensée pour répondre aux besoins concrets des PME. Collabix centralise l'ensemble des activités internes : espaces de travail par département, gestion des projets et des tâches, base de connaissances structurée et journaux de passation. La plateforme enrichit ces fonctionnalités par des capacités d'intelligence artificielle, notamment l'analyse automatisée des passations via le module Handover AI, et met à disposition un assistant métier intelligent orchestrant plusieurs modules dédiés à l'analyse, à la connaissance, à la passation et à la génération de rapports.

L'objectif général de ce travail est triple : centraliser les informations et les outils, renforcer la collaboration entre les équipes et introduire une couche d'intelligence artificielle au service de la prise de décision. Au-delà du cas particulier de l'agence, Collabix ambitionne de constituer une solution scalable, prête pour une exploitation SaaS, susceptible d'accompagner la transformation digitale d'autres PME.

Ce rapport s'articule autour de plusieurs chapitres. Après cette introduction, il présente le contexte et la problématique détaillée de l'étude, avant de définir les objectifs et le cahier des charges du projet. La conception de la solution, tant sur le plan architectural que fonctionnel, est ensuite exposée, suivie des choix d'implémentation technique. Les résultats obtenus sont enfin analysés et discutés, avant de conclure sur les perspectives d'évolution offertes par la plateforme.

---

## Informations générales

| Élément | Détail |
|---------|--------|
| **Titre du projet** | Collabix - Plateforme Collaborative Intelligente pour PME |
| **Entreprise** | Orchid Island Real Estate (Marrakech) |
| **Domaine** | Informatique / Développement Web Full-Stack |
| **Spécialité** | Développement de plateformes collaboratives avec IA |
| **Auteur** | [YOUR_NAME] |
| **Durée du stage** | 20 semaines |
| **Diplôme** | Informatique / Génie Logiciel |

---

# Chapitre 1 : Contexte et Problématique

## 1.1 Présentation de l'entreprise et contexte

Orchid Island Real Estate est une agence immobilière de prestige implantée à Marrakech, forte de plus de quinze années d'expertise sur le marché de l'immobilier haut de gamme. L'agence accompagne une clientèle locale et internationale dans l'acquisition, la location et la gestion de biens d'exception. Son positionnement exigeant impose une exigence particulière en matière de réactivité, de confidentialité et de qualité de service.

Sur le plan organisationnel, l'agence fonctionne avec plusieurs équipes dont les horaires sont décalés : une équipe opérationnelle le matin, une seconde le soir, ainsi que des services transverses (commercial, administratif, technique). Cette organisation en alternance vise à maximiser la disponibilité envers les clients, mais elle introduit une contrainte majeure : la coordination entre des équipes qui ne partagent que partiellement leurs plages horaires de travail.

**Figure 1 : Organigramme simplifié d'Orchid Island Real Estate**

```
                    Direction
                        │
        ┌───────────────┼───────────────┐
        │               │               │
   Équipe matin    Équipe soir    Services transverses
        │               │               │
   Conseillers     Conseillers     Comptabilité
   (vente/location) (vente/location)  / Admin / Technique
```

## 1.2 État des lieux des processus internes

L'observation du fonctionnement quotidien de l'agence révèle un écosystème d'outils variés, mais peu intégrés. Chaque canal de communication répond à un usage immédiat, sans vision globale.

### 1.2.1 Communication

Les échanges reposent principalement sur le courrier électronique, les messages courts et les appels téléphoniques. Aucun de ces canaux ne conserve un historique centralisé : une information transmise par téléphone peut n'exister nulle part ailleurs, et un échange important peut être enseveli dans une boîte mail personnelle.

### 1.2.2 Passations entre équipes

Les passations s'effectuent de manière informelle, à l'oral ou via des notes manuscrites transmises d'une équipe à l'autre. Le contenu transmis dépend alors de la mémoire et de la rigueur du collaborateur, et une partie de l'information se perd inévitablement au fil des rotations.

### 1.2.3 Gestion des tâches

Les tâches sont suivies dans des documents partagés, des carnets ou, plus simplement, dans la tête de chaque collaborateur. L'absence de visibilité temps réel empêche de savoir qui travaille sur quoi, à quel stade se trouve un dossier et quelles priorités doivent être traitées.

### 1.2.4 Documentation

Les procédures, les modèles et les référentiels métier sont éparpillés entre différents supports et différentes versions. Leur recherche est coûteuse en temps et leur fiabilité n'est pas garantie, faute de versioning centralisé.

### 1.2.5 Gestion des ressources humaines

L'attribution des rôles et des permissions repose sur une gestion manuelle. L'ajout ou le retrait d'accès n'est ni systématisé ni tracé, ce qui présente à la fois un risque opérationnel et un risque de sécurité.

**Tableau 1.1 : Synthèse des canaux et outils actuels**

| Processus | Outils actuels | Limites |
|-----------|----------------|---------|
| Communication | Email, SMS, appels | Aucun historique centralisé |
| Passation | Oral, notes manuscrites | Perte d'information, non structurée |
| Gestion des tâches | Tableurs, carnets | Aucune visibilité temps réel |
| Documentation | Fichiers dispersés | Versions multiples, recherche difficile |
| RH et accès | Gestion manuelle | Rôles et permissions non tracés |

**Figure 2 : Flux de communication actuel**

```
Équipe matin ── email/SMS/appels ──┐
                                  ├──> Fragmentation (pas d'historique)
Équipe soir ── notes manuscrites ─┘
```

## 1.3 Problèmes spécifiques identifiés

L'analyse de cet état des lieux permet d'identifier cinq problèmes structurants qui pénalisent l'agence au quotidien.

**Communication fragmentée.** La multiplicité des canaux rend impossible la reconstitution fiable d'un échange. Lorsqu'un dossier est transféré, le nouveau responsable doit reconstituer l'historique à partir d'éléments épars, avec le risque d'en manquer une partie.

**Passations non structurées.** Les informations de passation — dossiers en cours, décisions prises, engagements clients, points de vigilance — sont transmises de façon incomplète et inégale. Ce phénomène s'aggrave avec la rotation des équipes et les départs.

**Gestion des tâches sans visibilité.** Sans outil centralisé, il est impossible d'obtenir une vision d'ensemble des activités. La répartition de la charge de travail est inégale, les priorités sont floues et certaines tâches sont oubliées ou dupliquées.

**Documentation dispersée.** La capitalisation du savoir est insuffisante : les bonnes pratiques, les procédures validées et les référentiels ne sont ni structurés ni versionnés, ce qui freine la montée en compétence des collaborateurs.

**Automatisation et analyse absentes.** Les processus répétitifs (génération de rapports, suivi de dossiers, synthèses) sont effectués manuellement, et les données internes ne sont pas exploitées pour éclairer les décisions de gestion.

### 1.3.1 Impacts sur l'organisation

Ces problèmes ne sont pas sans conséquences mesurables pour l'agence :

- **Pertes de temps liées à la coordination** : chaque bascule entre équipes mobilise du temps pour rechercher, recouper et retransmettre l'information ;
- **Erreurs dues à des informations incomplètes** : un contexte partiel conduit à des décisions approximatives et à des engagements mal suivis ;
- **Manque de productivité** : les collaborateurs consacrent une part significative de leur temps à des tâches de recherche et de coordination plutôt qu'à leur cœur de métier ;
- **Onboarding difficile** : l'intégration d'un nouveau collaborateur est ralentie par l'absence de référentiel clair, de documentation centralisée et d'historique consultable.

## 1.4 Enjeux et justification du projet

Les difficultés observées ne sont pas spécifiques à cette agence : elles caractérisent de nombreuses PME qui dépassent le stade où quelques collaborateurs peuvent se coordonner de mémoire. À ce stade de croissance, l'organisation a besoin d'outils modernes structurant l'information, les rôles et les responsabilités.

La transformation digitale n'est plus une option, mais un facteur de compétitivité. Centraliser les activités internes, donner une visibilité temps réel et fiabiliser les passations constituent des gains immédiats de productivité et de qualité de service. Par ailleurs, l'émergence des modèles d'intelligence artificielle offre une opportunité nouvelle : automatiser l'analyse des passations, structurer la connaissance et générer des rapports décisionnels, autant de tâches jusqu'ici exclusivement manuelles.

Le présent projet répond donc à un double enjeu. D'une part, il apporte une réponse opérationnelle concrète à un problème réel observé en entreprise. D'autre part, il vise à démontrer qu'une plateforme collaborative de type SaaS, enrichie par l'intelligence artificielle, peut constituer un levier de transformation durable et scalable pour les PME. C'est dans cette perspective que le développement de Collabix a été engagé, comme détaillé dans les chapitres suivants.