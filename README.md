## How to initialize the project

```sh
git clone ...
cd gcopdd
git submodule init
git submodule update
cd graal/compiler
../../mx/mx build   # if you have multiple JVM versions, this will fail
                    # and give you hints how to fix it
../../mx/mx ideinit
```
