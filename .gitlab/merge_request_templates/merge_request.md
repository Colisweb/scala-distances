## Conseils pour l'auteur

 - Faire de petites MR (pas plus d'1 ou 2j de travail), merger dans une branche intermédiaire si ça dépasse 2 jours
 - Ne pas hésiter à demander un avis sur une MR même si elle n'est pas encore mergeable
 - Commencer par soumettre une MR sur le domaine
 - Ne pas hésiter à proposer une MR avec du code pas encore appelé 
   (ex: domaine ou persistence avant API HTTP)
 
## Checklist pour l'auteur

 - [ ] Le titre de la MR est clair
 - [ ] Le lien vers la [tâche]() est présent
 - [ ] Les tests sont clairs et au bon niveau de la pyramide
 - [ ] Les changements sont relus
 - [ ] Le code est formaté (scalafmt)
 - [ ] Le code a été déployé et vérifié sur testing
 - [ ] La documentation du README a été mise à jour (C4, BDD)
 - [ ] Les [critères d'acceptation des fonctions](https://www.notion.so/colisweb/Nous-mettons-en-place-des-crit-res-d-acceptation-d-une-fonction-Scala-27b4df9e21c2447d929ef71d2d4583e2)
       sont respectés 
 - [ ] Les tests de sérialisation ont été mis à jour si le code des codecs a évolué;
       ces tests sont cohérents avec les exemples fournis dans la spéc technique
 
## Conseils pour le relecteur

 - Quand il y a besoin d'explication privilégier le face à face
 - Critiquer le code et non la personne qui a codé
 
## Checklist pour le relecteur

 - [ ] Vérifier la compréhension de specs avant de lire le code
 - [ ] Vérifier que le code est vraiment compréhensible, sinon risque de mauvaise application/compréhension des specs
