#!/usr/bin/groovy
package com.gdg

def main(script) {
    // Pipeline specific variable get from injected env
    // Mandatory variable will be echeck at details & validation steps
    def String branch_name = ("${script.env.branch_name}" != "null") ? "${script.env.branch_name}" : ""
    def String package_name = ("${script.env.package_name}" != "null") ? "${script.env.package_name}" : ""

    ansiColor('xterm') {
        stage('Git Checkout') {
            println("==================Cloning Code==================")
            git branch: "${branch_name}", url: "https://github.com/dimasmamot/${package_name}.git"
            println("==================Success Cloning Code==================")
        }
        stage('Building') {
            println("==================Building Code==================")
            dir('website'){
                nodejs(cacheLocationStrategy: workspace(), nodeJSInstallationName: 'nodejs') {
                    sh 'yarn install && yarn run build'
                }
            }
        }
        stage('Upload Artifact'){
            println("==================Building Code==================")
            dir('website'){
                sh 'rm -rf build-*.tar.gz'
                sh "tar czf build-${BUILD_NUMBER}.tar.gz build/*"
                sh 'tar czf build-latest.tar.gz build/*'
                googleStorageUpload bucket: "gs://artifact-gdg-demo/$package_name", credentialsId: 'gdg-demo-290501', pattern: 'build-*.tar.gz'
            }
        }
        stage('Deploy') {
            println("==================Cloning Ansible==================")
            git 'https://github.com/hafizhanindito/ansible-gdg-demo.git'
            println("==================Success Cloning Ansible==================")

            println("==================Deploying Code==================")
            println("Executing ansible to deploy")
            withPythonEnv('System-CPython-2.7') {
                sh 'pip install ansible'
                dir('playbooks'){
                    ansiblePlaybook colorized: true, credentialsId: '3cf031db-f7b1-4c50-b58b-7d36b3c69718', disableHostKeyChecking: true, extras: "-e 'pkg_name=$package_name' -e 'pkg_version=$BUILD_NUMBER'", installation: 'ansible-workspace', inventory: '../inventories/gcp/gdg-demo/asia-southeast2/production/inventory.ini', playbook: "${package_name}.yml", tags: 'update'
                }
            }
        }
    }
}

return this