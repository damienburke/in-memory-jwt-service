### Goals of in-memory-jwt-service:

* Generate and maintain one pair of RSA private/public keys during the life time of the service, and serve the public key as JWKS.
* When requested, generate JWTs for given user and authorities, with the JWTs signed by the private key.

### Usages

```
$ curl -X POST \
    http://localhost:8008/jwt \
    -H 'Content-Type: application/json' \
    -d '{
      "authorities": ["shopper", "admin"],
      "user": {
        "id": "dkhw98dojf",
        "email": "em@ail"
    },
    "expireAfterSeconds": 3600
  }'
```
It will respond with:
```
{
  "jwt" : "eyJraWQiOiJNT0NLX0tFWV9JRCIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJhY2NvdW50SWQiOiIyNzg2MzQ4NzI2IiwiYnJva2VySWQiOiI5MDh1b2lqcXdyIiwiaWRlbnRpdHlUeXBlIjoidXNlciIsIjJmYS1hdXRoZW50aWNhdGVkIjpmYWxzZSwiZnJvemVuIjpmYWxzZSwiYWNjcmVkaXRlZCI6ZmFsc2UsImV4cCI6MTU2NzYzODM5MCwiaWF0IjoxNTY3NjM0NzkwLCJ1c2VySWQiOiJka2h3OThkb2pmIiwiYXV0aG9yaXRpZXMiOlsiYXBpOmNhbi1hY2gtZGVwb3NpdCIsIm1qczpjYW4tYXV0aCJdLCJqdGkiOiJlYmI0MTgyNy0yZDFhLTQ3M2QtYjliOS00N2M4ZWE5Mjg1NmMiLCJlbWFpbCI6ImVtQGFpbCJ9.EIQtsmONGdgkp_I9gVhl35OkTIL2m65Wwaz92PomXDOLNB9t_WdXBXvxTb47raePPv6WgdtnPCkbp9OTGNRb5lu5cu7-hRbIPoWheyIIG3-kXat-5rdIKfsS1LsnCnPtgUzirKSq7ujJQjAjm4z_PiBS_7saHNJj7L5DyQ6-tMg_QqH6LzqC05JnvgP54sSi4cDGO3lOKDZlO5G4daDvWh03epjWoJgeHxQy6-NXth03TCPwOOwT9Zfo8RuAOPQuhnYeUqL7nD04xAjctH-rNs3jSyI8rty_djBhix11SsqUt3H5qAz9OD7RH_6nXEBFHkpJ9UsmjD9GGLqqNzYCog"
}
```

#### JWKS

```
$ curl -X GET http://localhost:8008/.well-known/jwks.json
```
