#!/usr/bin/R --vanilla -q --slave -f

# this file is a demo of how lars works for variable selection
# in multivariate models
use <- paste(
        "lars.r -nvar n -in inFile -rmColumns col1,col2,... -responseTag tag ",
        "      [-nInputRows n]\n",
        "-nvar...number of linear coeficients; default=nCovariates/2\n",
        "-rmColumns remove columns with given headers\n",
        "-in.....Name of tab separated file,\n",
        "        the first row contains column tags (header)\n",
        "        The column with responseTag header contains response values\n",
        "        All other columns are treaded as descriptors.\n",
        "        Columns with more than 50% NA will be removed.\n",
        "        Rows with any NA will be removed.\n",
        "-nInputRows number of rows in the total dataset, used for scoring\n",
        "-responseTagheader of column containing res[ponse variable\n",
        "\n")

###################################################################
# execution on windows with R 2.6
# c:\Programs\R\R-2.6.0\bin\R.exe --vanilla -q --slave -f lars.r \
#                                 --args -nvar 5 -in diabetes.tab -responseTag y
#
# execution on linux with R 2.2
# R --vanilla -q --slave --args -nvar 5 -in diabetes.tab -responseTag y \
#   < lars.r

exit<-function(status, msg="")
{  if(msg != "") cat(msg,"\n\n")
   if(status != 0) cat(use)
   quit(status=status)
}

nvar<-0
inFile<-"in.tab"
responseTag<-"act"
nInputRows<-0

#in R 2.6 we could replace the follwont two lines with args<-commandArgs(TRUE)
args<-commandArgs()
args<-args[min(grep("^--args$",args)+1):length(args)]
#args<-c("-in","c:\\tmp\\tmp.2.3552.tab","-nvar",3,"-rmColumns","SMILES,AC_NUMBER",
#        "-responseTag", "IC50")

#args<-c("-nvar", 4,"-in","diabetes.tab","-responseTag","y")
while(length(args) > 0)
{  if(length(args)<2) exit(1) 

   if(args[1] == "-in")
   {  inFile<- args[2]
      args<-args[-2:-1]
      next
   }
   if(args[1] == "-nvar")
   {  nvar<- as.integer(args[2])
      args<-args[-2:-1]
      next
   }
   if(args[1] == "-responseTag")
   {  responseTag<- args[2]
      args<-args[-2:-1]
      next
   }
   if(args[1] == "-rmColumns")
   {  rmCols<- args[2]
      args<-args[-2:-1]
      next
   }
   if(args[1] == "-nInputRows")
   {  nInputRows<-as.numeric(args[2])
      args<-args[-2:-1]
      next
   }

   exit(1, paste("unknown argument:",args[1]))
}

if(! exists("inFile")) inFile<-"in.tab"

inp<-read.table(inFile,header=T,sep="\t", quote="", comment.char="")

#remove columns as from rmColumns
if(exists("rmCols"))
{  rmCols<-gsub(",","|",rmCols)
   rmCols<-paste("^(", rmCols, ")$" ,sep="")
   inp<-inp[,-grep(rmCols,colnames(inp))]
}

if(length(grep(paste("^",responseTag,"$",sep=""),colnames(inp))) == 0)
   exit(1,paste("Could not find response column:", responseTag))

if(ncol(inp) < 2)
  exit(1,"No columns in input file")

# extract activity column
act<-inp[,responseTag]
inp<-as.matrix(inp[,colnames(inp)!=responseTag])

if(nInputRows == 0 ) nInputRows<-length(act)

#remove columns with less than 50% data
#inp
if(sum(colSums(is.na(inp))>=nrow(inp)/2) > 0)
{  cat("Warning: removing columns with no to many missing values:",
       colnames(inp[,colSums(is.na(inp))>=nrow(inp)/2]),"\n")
   inp<-inp[,colSums(is.na(inp))<nrow(inp)/2]
}

if(nrow(inp)<nvar+1)
   exit(1, paste("There are only", nrow(inp), 
           "rows for", nvar, "coeficients"))

#remove descriptors with no variance
if(sum(apply(inp,2,var,na.rm=TRUE)==0) > 0)
{ cat("Warning: removing columns with no variance:",
      colnames(inp)[apply(inp,2,var,na.rm=TRUE)==0],"\n")
   inp<-inp[,apply(inp,2,var)!=0]
}

if(ncol(inp)==0)
   exit(1,"There are no descriptors left!") 

if(nvar == 0) nvar<-as.integer((length(inp[1,])+.5)/2)

#remove rows with NA values
act<-act[rowSums(is.na(inp))==0]
inp<-inp[rowSums(is.na(inp))==0,]

nOKRows<-length(act)

if(nvar > ncol(inp))
{  cat("Warning: There are less columns than the number requested\n")
   nvar<-ncol(inp)
}

# check again if there are enough rows
if(nrow(inp)<nvar+1)
   exit(1, paste("There are only", nrow(inp), 
           "rows for", nvar, "coeficients"))

if(var(act) == 0)
  exit(1,"Variance of response variable is 0")


library("lars")

# extract nvar most important variables and create 
# (ordinary) linear model OLS. The 
f<-function(orgModel=FALSE, act, inp, nvar, method)
{   tryCatch(
    {  lModel2<-lars(inp,act,type=method,max.steps=nvar)
       if(typeof(orgModel) != "list" || 
            orgModel$R2[length(orgModel$R2)] < lModel2$R2[length(lModel2$R2)])
          orgModel<-lModel2
    }, finally=return(orgModel))
}
larsModel<-FALSE
nvar<-nvar+1
while(typeof(larsModel) != "list" && nvar > 0)
{  nvar<-nvar-1
   larsModel<-f(larsModel,act,inp,nvar,"lar")
   larsModel<-f(larsModel,act,inp,nvar,"lasso")
   larsModel<-f(larsModel,act,inp,nvar,"stepwise")
   larsModel<-f(larsModel,act,inp,nvar,"forward.stagewise")
}

# get names of variables which are found to be important
cof<-coef(larsModel)
mvars<-nrow(cof)
cof<-cof[mvars,]
cof<-signif(cof[cof!=0],3)
vNames<-rownames(as.matrix(cof))
larsFormula<-paste(signif(mean(act),3), "+", 
                   paste(vNames,cof, sep= " * ", collapse=" + "))
larsR2<-cor(act,predict.lars(larsModel,inp,mvars,type="fit")$fit)^2
larsQual<-larsR2 * nOKRows/nInputRows * nOKRows/nInputRows

# now lets create a ordinary linear model using the same variables,
# according to Bradley Efron et.al this this will always increase
# the usual empirical R2 measure of fit (though not necessarily 
# the true fitting accuracy)
ols<-lm(act ~ inp[,vNames])
#summary(ols)
cof<-signif(coef(ols),3)
olsFormula<-paste(cof[1], "+", 
                  paste(vNames,cof[-1], sep= " * ", collapse=" + "))
#compute r^2
olsR2<-cor(act,predict(ols,as.data.frame(cbind(inp[,vNames],act))))^2
olsQual<-olsR2 * nOKRows/nInputRows * nOKRows/nInputRows

cat(larsFormula," qual=",signif(larsQual,3),
                " R2=",round(larsR2,3)," nRow=",nOKRows,"\n",
    olsFormula," qual=", signif(olsQual,3),
               " R2=",round(olsR2,3)," nRow=",nOKRows,"\n",
    sep="")
exit(0)


#here are some examples on how to plot the models

#predict ols by hand
f<-function(x,coefi) sum(x*coefi[-1])+coefi[1]
plot(act,apply(inp[,vNames],1,f,coef(ols)) )

#predict ols using predict
plot(act,predict(ols,inp[,vNames]))

plot(act,predict.lars(larsModel,inp,2,type="fit")$fit)
plot(act,predict.lars(larsModel,inp,3,type="fit")$fit)
plot(act,predict.lars(larsModel,inp,4,type="fit")$fit)
plot(act,predict.lars(larsModel,inp,5,type="fit")$fit)

cor(act,apply(inp[,vNames],1,f,coef(ols)))
cor(act,predict(ols,inp[,vNames]))
#correlation coeficient for lars predictors for steps 1-nvar
# staps start at 2 acctually because of intercept
cor(act,predict.lars(larsModel,inp,2:(nvar+1),type="fit")$fit)

#predicting lars by hand (no normalization needed)
#plot(act,apply(inp,1,f,c(mean(act),coef(larsModel)[nvar+1,])) )
