import getpass
import os
import pysftp
import subprocess


def check(filepath):
    if not os.path.exists(filepath):
        print(
            "'{}' not found. Make sure you're in the project root directory.".format(
                filepath
            )
        )
        return False
    return True


def confirm(prompt):
    while True:
        try:
            confirmation = input(prompt).lower().strip()
            if confirmation == "y":
                return True
            if confirmation == "n":
                return False
        except EOFError:
            return False


def rmdir_r(sftp, dirpath):
    for ent in sftp.listdir(dirpath):
        ent = os.path.join(dirpath, ent)
        if sftp.isdir(ent):
            rmdir_r(sftp, ent)
        else:
            sftp.remove(ent)
    sftp.rmdir(dirpath)



def main():
    if not (check("pom.xml") and check("src/main/java/agent/MyNDaysNCampaignsAgent.java")):
        return

    uname = input("CS Login: ")
    pwd = getpass.getpass()

    with pysftp.Connection("ssh.cs.brown.edu", username=uname, password=pwd) as sftp:
        print("Login successful.")
        print("Cleaning project.")
        subprocess.run(["mvn", "clean"])
        dirpath = os.path.join("/course/cs1951k/student", uname, "AdXFinal")
        if sftp.exists(dirpath):
            if not confirm(
                "WARNING: '{}' already exists. Continuing will completely overwrite it. Make sure your code is backed up elsewhere. Continue (y/n)? ".format(
                    dirpath
                )
            ):
                return
            rmdir_r(sftp, dirpath)
        sftp.makedirs(dirpath)
        print("Transferring files.")
        sftp.chdir(dirpath)
        sftp.put_r(".", ".")

    print("Done.")


if __name__ == "__main__":
    main()
