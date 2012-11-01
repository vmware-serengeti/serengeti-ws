library(plyr)
library(ggplot2)

args <- commandArgs(TRUE)
dat <- args[1]
d <- read.table(file=dat, sep="\t", comment.char="", quote="", na.strings="NULL", header=TRUE, encoding="UTF8")

dim(d)
head(d)
nrow(d)

d1 <- ddply(d, ~uid, summarise, median=median(similarity), stdev=sd(similarity))
d2 <- merge(d, d1, by=1)
d2$stdev[is.na(d2$stdev)] <- 0

title <- "Distribution of Tweet Similarity\n(per uid)"
qplot(similarity, median, data=d2, alpha=I(1/2), geom=c("point", "smooth"), colour=stdev, ylab="median(similarity) per uid", main=title)

title <- "Histogram of Tweet Similarity\n(per metric)"
qplot(similarity, data=d2, geom="histogram", binwidth=.01, ylab="# uid", main=title)
