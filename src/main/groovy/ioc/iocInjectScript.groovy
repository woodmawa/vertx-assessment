package ioc


import io.micronaut.context.ApplicationContext

ApplicationContext context = ApplicationContext.run()


Vehicle v = context.run().getBean(Vehicle)

println v.start()