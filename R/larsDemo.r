# this file is a demo of how lars works for variable selection
# in multivariate models

args<-commandArgs(TRUE)
if(length(args) == 1) inFile<-args[1]
if(! exists("inFile")) inFile<-"in.tab"

inp<-read.table(inFile,header=T,sep="\t", quote="")
row.names(inp)<-inp[,1]
inp<-as.matrix(inp[,-1])

act<-inp[,1]
inp<-as.matrix(inp[,-1])


library("lars")
data(diabetes)
attach(diabetes)

# lars applied to diabetes data,
# graph shows varaible with most important emerging from 0
# on the left
object2 <- lars(x,y,type="lar")
plot(object2)
object2

# extract nvar=4 most important varables and create 
# (ordinary) linear model OLS. The 
nvar<-4
object<-lars(x,y,type="lar",max.steps=nvar)
cof<-coef(object)[nvar+1,]
cof<-cof[cof!=0]
vNames<-rownames(as.matrix(cof))
ols<-lm(y ~ x[,vNames])
summary(ols)

par(mfrow=c(3,2))

#predict ols by hand:
f<-function(x,coefi) sum(x*coefi[-1])+coefi[1]
plot(y,apply(x[,vNames],1,f,coef(ols)) )

#predict ols using predict
plot(y,predict(ols,x[,vNames]))

plot(y,predict.lars(object,x,2,type="fit")$fit)
plot(y,predict.lars(object,x,3,type="fit")$fit)
plot(y,predict.lars(object,x,4,type="fit")$fit)
plot(y,predict.lars(object,x,5,type="fit")$fit)

cor(y,apply(x[,vNames],1,f,coef(ols)))
cor(y,predict(ols,x[,vNames]))
#correlation coeficient for lars predictors for steps 1-nvar
# staps start at 2 acctually because of intercept
cor(y,predict.lars(object,x,2:(nvar+1),type="fit")$fit)

#predicting lars by hand (no normalization needed)
#plot(y,apply(x,1,f,c(mean(y),coef(object)[nvar+1,])) )
